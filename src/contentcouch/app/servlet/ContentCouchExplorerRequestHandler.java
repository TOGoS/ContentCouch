package contentcouch.app.servlet;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseArguments;
import togos.mf.base.BaseResponse;
import togos.mf.value.Arguments;
import togos.mf.value.Blob;
import togos.swf2.SwfBaseRequest;
import togos.swf2.SwfFrontRequestHandler;
import togos.swf2.SwfNamespace;
import contentcouch.activefunctions.Explorify;
import contentcouch.explorify.BaseUriProcessor;
import contentcouch.explorify.UriProcessor;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.rdf.DcNamespace;
import contentcouch.repository.MetaRepoConfig;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;

public class ContentCouchExplorerRequestHandler extends SwfFrontRequestHandler {
	static class ServletUriProcessor extends BaseUriProcessor {
		String pathToWebRoot;
		public ServletUriProcessor( UriProcessor postProcessor, boolean shouldRewriteRelativeUris, String pathToWebRoot ) {
			super( postProcessor, shouldRewriteRelativeUris );
			this.pathToWebRoot = pathToWebRoot;
		}
		
		protected String processServletUri(String uri) {
			uri = uri.substring(SwfNamespace.SERVLET_PATH_URI_PREFIX.length());
			if( uri.startsWith("/") ) uri = uri.substring(1);
			return pathToWebRoot + uri;
		}
		
		public String processExternalUri(String uri) {
			return uri;
		}
		
		public String processUri(String uri) {
			if( uri.startsWith(SwfNamespace.SERVLET_PATH_URI_PREFIX) ) {
				return processServletUri(uri);
			} else {
				return processExternalUri( uri );
			}
		}
	}
	
	protected MetaRepoConfig metaRepoConfig = new MetaRepoConfig();
	protected String webRoot;
	
	public void init( String configFileUri, String webRoot ) {
		this.webRoot = webRoot;
		TheGetter.globalInstance = metaRepoConfig.getRequestKernel();
		metaRepoConfig.handleArguments(new String[]{"-file",configFileUri}, 0, configFileUri);
		//contentcouch.app.Log.setStandardLogLevel( 60 );
	}

	protected String getProcessingUri(String processor, String uri, String verb) {
		if( uri == null ) {
			throw new RuntimeException( "Null URI given to getProcessingUri" );
		}
		return "(" + processor + " " + uri + ")";
	}
	protected String getExploreUri(String uri) {
		return getProcessingUri("contentcouch.explorify", uri, "Exploring");
	}

	public Response call( Request req ) {
		String pi = req.getResourceName();
		if( !pi.startsWith(SwfNamespace.SERVLET_PATH_URI_PREFIX) ) {
			throw new RuntimeException("Expected " + SwfNamespace.SERVLET_PATH_URI_PREFIX + "..., but got " + pi);
		}
		pi = pi.substring(SwfNamespace.SERVLET_PATH_URI_PREFIX.length());
		
		Arguments args = null;
		if( req.getContent() instanceof Arguments ) {
			args = (Arguments)req.getContent();
		}
		
		String _pathToRoot = "";
		for( int i=1; i<pi.length(); ++i ) {
			if( pi.charAt(i) == '/' ) _pathToRoot += "../";
		}
		final String pathToRoot = _pathToRoot;

		final boolean shouldRewriteRelativeUris;
		String uri = null;
		String[] pathComp = pi.substring(1).split("/");
		String inputUri = (String)args.getNamedArguments().get("uri");
		String defaultUriProcessorName = "raw";
		
		if( "process".equals(pathComp[0]) ) {
			shouldRewriteRelativeUris = true;
		    String processor = (String)args.getNamedArguments().get("processor");
			uri = getProcessingUri(processor, inputUri, "Album view of");
		} else if( "explore".equals(pathComp[0]) ) {
			defaultUriProcessorName = "explore";
		    if( inputUri != null ) {
				shouldRewriteRelativeUris = true;
			} else if( pi.startsWith("/explore/") ) {
				inputUri = PathUtil.appendPath("x-ccouch-repo://", pi.substring(9));
				shouldRewriteRelativeUris = false;
			} else {
				// TODO: redirect
				return new BaseResponse( ResponseCodes.RESPONSE_DOESNOTEXIST, "" );
			}
			uri = getExploreUri(inputUri);
		} else if( "album".equals(pathComp[0]) ) {
			defaultUriProcessorName = "album";
		    if( inputUri != null ) {
				shouldRewriteRelativeUris = true;
			} else if( pi.startsWith("/album/") ) {
				inputUri = PathUtil.appendPath("x-ccouch-repo://", pi.substring(7));
				shouldRewriteRelativeUris = false;
			} else {
				// TODO: redirect
				return new BaseResponse( ResponseCodes.RESPONSE_DOESNOTEXIST, "" );
			}
		    uri = getProcessingUri("contentcouch.photoalbum.album-page", inputUri, "Viewing");
		} else if( "raw".equals(pathComp[0]) ) {
			defaultUriProcessorName = "raw";
			if( inputUri != null ) {
				shouldRewriteRelativeUris = true;
			} else if( pi.startsWith("/raw/") ) {
				inputUri = PathUtil.appendPath("x-ccouch-repo://", pi.substring(5));
				shouldRewriteRelativeUris = false;
			} else {
				// TODO: redirect
				return new BaseResponse( ResponseCodes.RESPONSE_DOESNOTEXIST, "" );
			}
			uri = inputUri;
		} else if( pi.equals("/") ) {
			uri = PathUtil.appendPath(webRoot, "index.html");
			shouldRewriteRelativeUris = false;
		} else {
			uri = PathUtil.appendPath(webRoot, pi.substring(1));
			shouldRewriteRelativeUris = false;
		}
		
		BaseArguments subArgs;
		if( req.getContent() instanceof Arguments ) {
			subArgs = new BaseArguments( (Arguments)req.getContent() );
		} else {
			subArgs = new BaseArguments();
		}
		subArgs.putNamedArgument("uri", inputUri);
		SwfBaseRequest subReq = new SwfBaseRequest(req, uri);
		subReq.content = subArgs;
		subReq.putMetadata(CcouchNamespace.REQ_CACHE_SECTOR, "webcache");
		subReq.putAllConfig(metaRepoConfig.config, true);
		BaseUriProcessor rawUriProcessor = new ServletUriProcessor(BaseUriProcessor.getInstance(req, "explore"), shouldRewriteRelativeUris, pathToRoot) {
			public String processExternalUri(String uri) {
				return pathToRoot + "raw?uri=" + UriUtil.uriEncode(uri);
			}
		};
		BaseUriProcessor exploreUriProcessor = new BaseUriProcessor(BaseUriProcessor.getInstance(req, "explore"), shouldRewriteRelativeUris) {
			public String processUri(String uri) {
				return pathToRoot + "explore?uri=" + UriUtil.uriEncode(uri);
			}
		};
		BaseUriProcessor albumUriProcessor = new BaseUriProcessor(BaseUriProcessor.getInstance(req, "raw"), shouldRewriteRelativeUris) {
			public String processUri(String uri) {
				return pathToRoot + "album?uri=" + UriUtil.uriEncode(uri);
			}
		};
		
		if( "raw".equals(defaultUriProcessorName) ) {
			BaseUriProcessor.setInstance( subReq, "default", rawUriProcessor);
		} else if( "album".equals(defaultUriProcessorName) ) {
			BaseUriProcessor.setInstance( subReq, "default", albumUriProcessor);
		} else {
			BaseUriProcessor.setInstance( subReq, "default", exploreUriProcessor);
		}
		BaseUriProcessor.setInstance( subReq, "explore", exploreUriProcessor);
		BaseUriProcessor.setInstance( subReq, "raw", rawUriProcessor);
		BaseUriProcessor.setInstance( subReq, "album", albumUriProcessor);
		Response subRes = TheGetter.call(subReq);
		
		if( subRes.getContent() instanceof Directory ) {
			subRes = new Explorify().explorifyDirectory( subReq, (Directory)subRes.getContent() );
		}
		
		BaseResponse res = new BaseResponse(subRes);

		Number refreshi = ValueUtil.getNumber(args.getNamedArguments().get("refresh-interval"), null);
		if( refreshi != null ) {
			res.putMetadata(SwfNamespace.RES_HTTP_EQUIV + "Refresh", refreshi.toString());
		}
		
		String type = ValueUtil.getString(subRes.getContentMetadata().get(DcNamespace.DC_FORMAT));
		if( type == null && subRes.getContent() instanceof Blob ) {
			type = MetadataUtil.guessContentType((Blob)subRes.getContent());
			res.putContentMetadata(DcNamespace.DC_FORMAT, type);
		}
		return res;
	}
}
