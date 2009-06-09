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
import contentcouch.blob.BlobUtil;
import contentcouch.explorify.BaseUriProcessor;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.DcNamespace;
import contentcouch.repository.MetaRepoConfig;
import contentcouch.store.TheGetter;

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

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String pi = request.getPathInfo();
		if( pi == null ) pi = request.getRequestURI();
		if( pi == null ) pi = "/";
		
		try {
			final boolean shouldRewriteRelativeUris;
			String uri = null;
			if( pi.equals("/explore") ) {
				uri = "active:contentcouch.explorify+operand@" + UriUtil.uriEncode(request.getParameter("uri"));
				shouldRewriteRelativeUris = true;
			} else if( pi.startsWith("/explore/") ) {
				uri = "active:contentcouch.explorify+operand@" + UriUtil.uriEncode(PathUtil.appendPath("x-ccouch-repo://", pi.substring(9)));
				shouldRewriteRelativeUris = false;
			} else if( pi.equals("/") ) {
				uri = "file:web/_index.html";
				shouldRewriteRelativeUris = false;
			} else {
				uri = "file:web" + pi + ".html";
				shouldRewriteRelativeUris = false;
			}
			
			BaseRequest subReq = new BaseRequest(Request.VERB_GET, uri);
			BaseUriProcessor.push( new BaseUriProcessor(BaseUriProcessor.getInstance(), shouldRewriteRelativeUris) {
				public String processUri(String uri) {
					return "/explore?uri=" + UriUtil.uriEncode(uri);
				}
			});
			subReq.contextVars = Context.getInstance(); 
			Response subRes = TheGetter.handleRequest(subReq);
			
			response.setHeader("Content-Type", ValueUtil.getString(subRes.getContentMetadata().get(DcNamespace.DC_FORMAT)));
			BlobUtil.writeBlobToOutputStream( BlobUtil.getBlob( subRes.getContent() ), response.getOutputStream() );
		} catch( RuntimeException e ) {
			response.setHeader("Content-Type", "text/plain");
			e.printStackTrace(response.getWriter());
		}
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doGet(request, response);
	}
}
