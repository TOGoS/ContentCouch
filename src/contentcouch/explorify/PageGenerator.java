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
	public String header = "<html><body>";
	public String footer = "</body></html>";
	
	public PageGenerator( String uri, UriProcessor uriProcessor, String header, String footer ) {
		this.uri = uri;
		this.uriProcessor = uriProcessor;
		if( header != null ) this.header = header;
		if( footer != null ) this.footer = footer;
	}
	
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
	
	public void writeContent(PrintWriter w) {
		w.println("<p>Nothing to see here.</p>");
	}
	
	public void write(PrintWriter w) {
		w.println(header);
		writeContent(w);
		w.println(footer);
	}
	
	public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType(getContentType());
		write(response.getWriter());
	}
}