package contentcouch.explorify;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import contentcouch.app.servlet.ContentCouchExplorerServlet.HttpServletRequestHandler;
import contentcouch.misc.Function1;
import contentcouch.misc.ValueUtil;

public abstract class PageGenerator implements HttpServletRequestHandler {
	public Function1 uriProcessor;
	
	protected String processUri( String uri ) {
		return uriProcessor == null ? uri : ValueUtil.getString(uriProcessor.apply(uri));
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