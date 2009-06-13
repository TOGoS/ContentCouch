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
		System.err.println("001: " + configFileUri);
		metaRepoConfig.handleArguments(new String[]{"-file",configFileUri}, 0, ".");
		System.err.println("002");
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
	
	protected String getExploreUri(String uri) {
		return
			"(contentcouch.explorify\n" +
			"  " + uri + "\n" +
			"  header=(contentcouch.let\n" +
			"    vars/page-title=(contentcouch.concat\n" +
			"      \"Exploring \" x-context-var:explored-uri \"\")\n" +
			"    vars/page-title2=(contentcouch.concat\n" +
			"      \"Exploring \" x-context-var:explored-uri \"\")\n" +
			"    (contentcouch.eval\n" +
			"       (contentcouch.builtindata.get \"default-page-header-expression\"))\n" +
			"  )\n" +
			")\n";
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String pi = request.getPathInfo();
		if( pi == null ) pi = request.getRequestURI();
		if( pi == null ) pi = "/";
		
		try {
			final boolean shouldRewriteRelativeUris;
			String uri = null;
			String loadPath = null;
			if( pi.equals("/explore") ) {
				uri = getExploreUri(request.getParameter("uri"));
				shouldRewriteRelativeUris = true;
				loadPath = "/explore?uri=";
			} else if( pi.startsWith("/explore/") ) {
				uri = getExploreUri(PathUtil.appendPath("x-ccouch-repo://", pi.substring(9)));
				shouldRewriteRelativeUris = false;
				loadPath = "/explore?uri=";
			} else if( pi.equals("/raw") ) {
				uri = request.getParameter("uri");
				shouldRewriteRelativeUris = true;
				loadPath = "/raw?uri=";
			} else if( pi.startsWith("/raw/") ) {
				uri = PathUtil.appendPath("x-ccouch-repo://", pi.substring(5));
				shouldRewriteRelativeUris = false;
				loadPath = "/raw?uri=";
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
				final String fLoadPath = loadPath;
				BaseUriProcessor.push( new BaseUriProcessor(BaseUriProcessor.getInstance(), shouldRewriteRelativeUris) {
					public String processUri(String uri) {
						return fLoadPath + UriUtil.uriEncode(uri);
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
				}
			
				if( type != null ) response.setHeader("Content-Type", type);
				BlobUtil.writeBlobToOutputStream( BlobUtil.getBlob( subRes.getContent() ), response.getOutputStream() );
			} finally {
				BaseUriProcessor.pop();
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
