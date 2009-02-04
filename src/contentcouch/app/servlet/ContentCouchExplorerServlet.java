package contentcouch.app.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import contentcouch.app.ContentCouchRepository;
import contentcouch.data.Blob;
import contentcouch.data.BlobUtil;

public class ContentCouchExplorerServlet extends HttpServlet {
	protected ContentCouchRepository getRepo() {
		return new ContentCouchRepository("junk-repo");
	}
	
	public Object get(String path) {
		return getRepo().get(path);
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setHeader("Content-Type", "text/plain");
		
		String pi = request.getPathInfo();
		if( pi == null ) pi = request.getRequestURI();
		if( pi == null ) pi = "/";
		
		Object page = get(pi.substring(1));
		if( page == null ) page = "Nothing found!";
		if( page instanceof Blob ) {
			BlobUtil.writeBlobToOutputStream(((Blob)page), response.getOutputStream());
		} else {
			response.getWriter().println(page.toString());
		}
	}
}
