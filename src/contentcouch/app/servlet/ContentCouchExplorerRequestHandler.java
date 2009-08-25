package contentcouch.app.servlet;

import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import togos.mf.value.Arguments;
import togos.mf.value.Blob;
import togos.swf2.SwfFrontRequestHandler;
import togos.swf2.SwfHttpServlet;
import contentcouch.active.Context;
import contentcouch.activefunctions.Explorify;
import contentcouch.builtindata.BuiltInData;
import contentcouch.explorify.BaseUriProcessor;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.DcNamespace;
import contentcouch.repository.MetaRepoConfig;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;

public class ContentCouchExplorerRequestHandler extends SwfFrontRequestHandler {
	protected MetaRepoConfig metaRepoConfig = new MetaRepoConfig();
	protected String webRoot;
	
	public void init( String configFileUri, String webRoot ) {
		this.webRoot = webRoot;
		TheGetter.globalInstance = metaRepoConfig.getRequestKernel();
		metaRepoConfig.handleArguments(new String[]{"-file",configFileUri}, 0, configFileUri);
		contentcouch.app.Log.setStandardLogLevel( 60 );
	}

	protected String getProcessingUri(String processor, String uri, String verb) {
		if( uri == null ) {
			throw new RuntimeException( "Null URI given to getProcessingUri" );
		}
		return
			"(" + processor + "\n" +
			"  " + uri + "\n" +
			"  header=(contentcouch.let\n" +
			"    vars/page-title=(contentcouch.concat\n" +
			"      \"" + verb + " \" x-context-var:processed-uri \"\")\n" +
			"    vars/page-title2=(contentcouch.concat\n" +
			"      \"" + verb + " \" x-context-var:processed-uri \"\")\n" +
			"    (contentcouch.eval\n" +
			"       (contentcouch.builtindata.get \"default-page-header-expression\"))\n" +
			"  )\n" +
			")\n";
	}
	protected String getExploreUri(String uri) {
		return getProcessingUri("contentcouch.explorify", uri, "Exploring");
	}

	public Response call( Request req ) {
		String pi = req.getResourceName();
		if( !pi.startsWith(SwfHttpServlet.SERVLET_PATH_URI_PREFIX) ) {
			throw new RuntimeException("Expected " + SwfHttpServlet.SERVLET_PATH_URI_PREFIX + "..., but got " + pi);
		}
		pi = pi.substring(SwfHttpServlet.SERVLET_PATH_URI_PREFIX.length());
		
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
		if( "process".equals(pathComp[0]) ) {
			shouldRewriteRelativeUris = true;
		    String processor = (String)args.getNamedArguments().get("processor");
			uri = getProcessingUri(processor, inputUri, "Album view of");
		} else if( "explore".equals(pathComp[0]) ) {
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
		} else if( "raw".equals(pathComp[0]) ) {
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
			uri = PathUtil.appendPath(webRoot, "_index.html");
			shouldRewriteRelativeUris = false;
		} else {
			uri = PathUtil.appendPath(webRoot, pi.substring(1) + ".html");
			shouldRewriteRelativeUris = false;
		}
		
		BaseRequest subReq = new BaseRequest(RequestVerbs.VERB_GET, uri);
		try {
			Context.push("funk", "Bring the funk");
			BaseUriProcessor.push( "explore", new BaseUriProcessor(BaseUriProcessor.getInstance("explore"), shouldRewriteRelativeUris) {
				public String processUri(String uri) {
					return pathToRoot + "explore?uri=" + UriUtil.uriEncode(uri);
				}
			});
			BaseUriProcessor.push( "raw", new BaseUriProcessor(BaseUriProcessor.getInstance("raw"), shouldRewriteRelativeUris) {
				public String processUri(String uri) {
					return pathToRoot + "raw?uri=" + UriUtil.uriEncode(uri);
				}
			});
			BaseUriProcessor.push( "album", new BaseUriProcessor(BaseUriProcessor.getInstance("raw"), shouldRewriteRelativeUris) {
				public String processUri(String uri) {
					return pathToRoot + "process?processor=contentcouch.photoalbum.make-album-page&uri=" + UriUtil.uriEncode(uri);
				}
			});
			subReq.contextVars = Context.getInstance();
			Response subRes = TheGetter.handleRequest(subReq);
			
			if( subRes.getContent() instanceof Directory ) {
				subRes = Explorify.explorifyDirectory( uri, (Directory)subRes.getContent(),
					"<html><head><style>/*<!CDATA[*/\n" + BuiltInData.getString("default-page-style") + "/*]]>*/</style><body>\n", null );
			}
			
			BaseResponse res = new BaseResponse(subRes);
			
			String type = ValueUtil.getString(subRes.getContentMetadata().get(DcNamespace.DC_FORMAT));
			if( type == null && subRes.getContent() instanceof Blob ) {
				type = MetadataUtil.guessContentType((Blob)subRes.getContent());
				res.putContentMetadata(DcNamespace.DC_FORMAT, type);
			}
			return res;
		} finally {
			BaseUriProcessor.pop("raw");
			BaseUriProcessor.pop("explore");
			BaseUriProcessor.pop("album");
		}
	}
}
