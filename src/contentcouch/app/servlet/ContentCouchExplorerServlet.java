package contentcouch.app.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import togos.rra.BaseRequest;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.active.Context;
import contentcouch.activefunctions.Explorify;
import contentcouch.blob.BlobUtil;
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

public class ContentCouchExplorerServlet extends HttpServlet {
	public interface HttpServletRequestHandler {
		public void handle( HttpServletRequest request, HttpServletResponse response ) throws IOException;
	}
	
	protected MetaRepoConfig metaRepoConfig;

	protected void copyFile( File src, File dest ) throws IOException {
		FileInputStream is = new FileInputStream(src);
		FileOutputStream os = new FileOutputStream(dest);
		try {
			byte[] buf = new byte[512];
			int len;
			while( (len = is.read(buf)) > 0 ) {
				os.write(buf, 0, len);
			}
		} finally {
			is.close();
			os.close();
		}
	}
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		metaRepoConfig = new MetaRepoConfig();
		TheGetter.globalInstance = metaRepoConfig.getRequestKernel();
		File configFile = getConfigFile();
		String configFileUri = PathUtil.maybeNormalizeFileUri(configFile.getPath());
		metaRepoConfig.handleArguments(new String[]{"-file",configFileUri}, 0, ".");

		contentcouch.app.Log.setStandardLogLevel( 60 );
	}
	
	protected File getConfigFile() {
		String webPath = getServletContext().getRealPath("");
		File configFile = new File(webPath + "/repo-config");
		File configTemplateFile = new File(webPath + "/repo-config.template");
		if( !configFile.exists() ) {
			try {
				copyFile(configTemplateFile, configFile);
			} catch( IOException e ) {
				throw new RuntimeException("Failed to copy " + configTemplateFile.getPath() + " to " + configFile.getPath(), e);
			}
		}
		return configFile;
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
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String pi = request.getPathInfo();
		if( pi == null ) pi = request.getRequestURI();
		if( pi == null ) pi = "/";
		
		try {
			final boolean shouldRewriteRelativeUris;
			String uri = null;
			String[] pathComp = pi.substring(1).split("/");
			String inputUri = request.getParameter("uri");
			if( "process".equals(pathComp[0]) ) {
				shouldRewriteRelativeUris = true;
			    String processor = request.getParameter("processor");
				uri = getProcessingUri(processor, inputUri, "Album view of");
			} else if( "explore".equals(pathComp[0]) ) {
			    if( inputUri != null ) {
					shouldRewriteRelativeUris = true;
				} else if( pi.startsWith("/explore/") ) {
					inputUri = PathUtil.appendPath("x-ccouch-repo://", pi.substring(9));
					shouldRewriteRelativeUris = false;
				} else {
					// TODO: redirect
					return;
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
					return;
				}
				uri = inputUri;
			} else if( pi.equals("/") ) {
				uri = "file:web/_index.html";
				shouldRewriteRelativeUris = false;
			} else {
				uri = "file:web" + pi + ".html";
				shouldRewriteRelativeUris = false;
			}
			
			BaseRequest subReq = new BaseRequest(Request.VERB_GET, uri);
			try {
				Context.push("funk", "Bring the funk");
				BaseUriProcessor.push( "explore", new BaseUriProcessor(BaseUriProcessor.getInstance("explore"), shouldRewriteRelativeUris) {
					public String processUri(String uri) {
						return "/explore?uri=" + UriUtil.uriEncode(uri);
					}
				});
				BaseUriProcessor.push( "raw", new BaseUriProcessor(BaseUriProcessor.getInstance("raw"), shouldRewriteRelativeUris) {
					public String processUri(String uri) {
						return "/raw?uri=" + UriUtil.uriEncode(uri);
					}
				});
				BaseUriProcessor.push( "album", new BaseUriProcessor(BaseUriProcessor.getInstance("raw"), shouldRewriteRelativeUris) {
					public String processUri(String uri) {
						return "/process?processor=contentcouch.photoalbum.make-album-page&uri=" + UriUtil.uriEncode(uri);
					}
				});
				subReq.contextVars = Context.getInstance();
				Response subRes = TheGetter.handleRequest(subReq);
				
				if( subRes.getContent() instanceof Directory ) {
					subRes = Explorify.explorifyDirectory( uri, (Directory)subRes.getContent(),
						"<html><head><style>/*<!CDATA[*/\n" + BuiltInData.getString("default-page-style") + "/*]]>*/</style><body>\n", null );
				}
				
				String type = ValueUtil.getString(subRes.getContentMetadata().get(DcNamespace.DC_FORMAT));
				if( type == null && subRes.getContent() instanceof Blob ) {
					type = MetadataUtil.guessContentType((Blob)subRes.getContent());
					type = "text/html";
				}
				
				switch( subRes.getStatus() ) {
				case( Response.STATUS_NORMAL ): break;
				case( Response.STATUS_DOESNOTEXIST ): case( Response.STATUS_UNHANDLED ):
					response.sendError(404, "Resource Not Found"); break;
				case( Response.STATUS_USERERROR ):
					response.sendError(400, "User Error"); break;
				default:
					response.sendError(500, "RRA Error " + subRes.getStatus()); break;
				}
				if( type != null ) response.setHeader("Content-Type", type);
				BlobUtil.writeBlobToOutputStream( BlobUtil.getBlob( subRes.getContent() ), response.getOutputStream() );
			} finally {
				BaseUriProcessor.pop("raw");
				BaseUriProcessor.pop("explore");
				BaseUriProcessor.pop("album");
			}				
		} catch( RuntimeException e ) {
			response.setHeader("Content-Type", "text/plain");
			e.printStackTrace(response.getWriter());
		}
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doGet(request, response);
	}
}
