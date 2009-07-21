package contentcouch.explorify;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import contentcouch.active.Context;
import contentcouch.app.servlet.ContentCouchExplorerServlet.HttpServletRequestHandler;

public abstract class PageGenerator implements HttpServletRequestHandler {
	public String uri;
	public Map context;
	public String header = "<html><body>";
	public String footer = "</body></html>";
	
	public PageGenerator( String uri, Map context, String header, String footer ) {
		this.context = context;
		this.uri = uri;
		if( header != null ) this.header = header;
		if( footer != null ) this.footer = footer;
	}
	
	protected String processUri( String whichProcessor, String uri ) {
		return BaseUriProcessor.getInstance(whichProcessor).processUri(uri);
	}
	protected String processRelativeUri( String whichProcessor, String baseUri, String relativeUri ) {
		return BaseUriProcessor.getInstance(whichProcessor).processRelativeUri(baseUri, relativeUri);
	}
	
	////
	
	public String getContentType() {
		return "text/html; charset=utf-8";
	}
	
	public void writeContent(PrintWriter w) {
		w.println("<p>Nothing to see here.</p>");
	}
	
	public void write(PrintWriter w) {
		Context.pushInstance(context);
		try {
			w.println(header);
			writeContent(w);
			w.println(footer);
		} finally {
			Context.popInstance();
		}
	}
	
	public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType(getContentType());
		write(response.getWriter());
	}
}