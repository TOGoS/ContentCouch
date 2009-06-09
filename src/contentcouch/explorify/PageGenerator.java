package contentcouch.explorify;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import contentcouch.app.servlet.ContentCouchExplorerServlet.HttpServletRequestHandler;
import contentcouch.misc.ValueUtil;

public abstract class PageGenerator implements HttpServletRequestHandler {
	public String uri;
	public UriProcessor uriProcessor;
	
	protected String processUri( String uri ) {
		return uriProcessor == null ? uri : ValueUtil.getString(uriProcessor.processUri(uri));
	}
	protected String processRelativeUri( String baseUri, String relativeUri ) {
		return uriProcessor == null ? relativeUri : ValueUtil.getString(uriProcessor.processRelativeUri(baseUri, relativeUri));
	}
	
	////
	
	public String getContentType() {
		return "text/html; charset=utf-8";
	}
	
	public abstract void write(PrintWriter w) throws IOException;
	
	public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType(getContentType());
		write(response.getWriter());
	}
}