package contentcouch.explorify;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import togos.swf2.HttpServletRequestHandler;

public abstract class PageGenerator implements HttpServletRequestHandler
{
	public String getContentType() {
		return "text/html; charset=utf-8";
	}
	
	public abstract void write(PrintWriter w);
	
	public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType(getContentType());
		write(response.getWriter());
	}
}