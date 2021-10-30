package togos.swf2;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Objects of this type can be passed back as response content.
 *  They will manipulate the HttpServletResponse directly.
 *  This is useful when there could be a very large dynamically generated response
 *  that we do not want to have to buffer before sending it out. */
public interface HttpServletRequestHandler {
	public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
