package togos.swf2;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface HttpServletRequestHandler {
	public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
