package contentcouch.app.servlet;

import togos.rra.Arguments;
import togos.rra.BaseRequest;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;
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
import contentcouch.value.Blob;
import contentcouch.value.Directory;

public class ContentCouchExplorerRequestHandler extends SwfFrontRequestHandler {
	protected MetaRepoConfig metaRepoConfig = new MetaRepoConfig();
	protected String webRoot;
	
	public void init( String configFileUri, String webRoot ) {
		this.webRoot = webRoot;
		TheGetter.globalInstance = metaRepoConfig.getRequestKernel();
		metaRepoConfig.handleArguments(new String[]{"-file",configFileUri}, 0, configFileUri);
		contentcouch.app.Log.setStandardLogLevel( 60 );
		putComponent("explore", new ExploreComponent(SwfHttpServlet.SERVLET_PATH_URI_PREFIX + "/explore"));
		putComponent("album", new ExploreAlbumComponent(SwfHttpServlet.SERVLET_PATH_URI_PREFIX + "/album"));
		putComponent("raw", new ExploreRawComponent(SwfHttpServlet.SERVLET_PATH_URI_PREFIX + "/raw"));
		UriMapComponent webRootC = new UriMapComponent(SwfHttpServlet.SERVLET_PATH_URI_PREFIX + "/", webRoot, true, "_index");
		webRootC.addAutoPostfix(".html");
		putComponent("webroot", webRootC );
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

	public Response handleRequest( Request req ) {
		String pi = req.getUri();
		if( !pi.startsWith(SwfHttpServlet.SERVLET_PATH_URI_PREFIX) ) {
			throw new RuntimeException("Expected " + SwfHttpServlet.SERVLET_PATH_URI_PREFIX + "..., but got " + pi);
		}
		pi = pi.substring(SwfHttpServlet.SERVLET_PATH_URI_PREFIX.length());
		
		String _pathToRoot = "";
		for( int i=1; i<pi.length(); ++i ) {
			if( pi.charAt(i) == '/' ) _pathToRoot += "../";
		}
		final String pathToRoot = _pathToRoot;

		BaseRequest subReq1 = new BaseRequest(req);
		final boolean shouldRewriteRelativeUris = true;
		subReq1.putContextVar(BaseUriProcessor.CTXVAR + "/default", new BaseUriProcessor(null, shouldRewriteRelativeUris) {
			public String processUri(String uri) {
				return pathToRoot + "explore?uri=" + UriUtil.uriEncode(uri);
			}
		});
		subReq1.putContextVar(BaseUriProcessor.CTXVAR + "/explore", new BaseUriProcessor(null, shouldRewriteRelativeUris) {
			public String processUri(String uri) {
				return pathToRoot + "explore?uri=" + UriUtil.uriEncode(uri);
			}
		});
		subReq1.putContextVar(BaseUriProcessor.CTXVAR + "/raw", new BaseUriProcessor(null, shouldRewriteRelativeUris) {
			public String processUri(String uri) {
				return pathToRoot + "raw?uri=" + UriUtil.uriEncode(uri);
			}
		});
		subReq1.putContextVar(BaseUriProcessor.CTXVAR + "/album", new BaseUriProcessor(null, shouldRewriteRelativeUris) {
			public String processUri(String uri) {
				return pathToRoot + "process?processor=contentcouch.photoalbum.make-album-page&uri=" + UriUtil.uriEncode(uri);
			}
		});
		if( true ) return super.handleRequest(subReq1);
		
		// Old code temporarily left around for reference:
		
		Arguments args = null;
		if( req.getContent() instanceof Arguments ) {
			args = (Arguments)req.getContent();
		}

		String uri = null;
		String[] pathComp = pi.substring(1).split("/");
		String inputUri = (String)args.getNamedArguments().get("uri");
		
		BaseRequest subReq = new BaseRequest(Request.VERB_GET, uri);
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
