package togos.rra;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class MultiRequestHandler extends BaseRequestHandler {
	protected List requestHandlers = new ArrayList();
	
	public void addRequestHandler(RequestHandler requestHandler) {
		requestHandlers.add(0, requestHandler);
	}

	public Response handleRequest(Request request) {
		Response res;
		for( Iterator i=requestHandlers.iterator(); i.hasNext(); ) {
			res = ((RequestHandler)i.next()).handleRequest(request);
			if( res.getStatus() > 0 ) return res;
		}
		return BaseResponse.RESPONSE_UNHANDLED;
	}
}
