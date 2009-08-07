package contentcouch.framework;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import togos.mf.api.Request;
import togos.mf.api.RequestHandler;
import togos.mf.api.Response;
import togos.mf.base.BaseResponse;


public class MultiRequestHandler extends BaseRequestHandler {
	protected List requestHandlers = new ArrayList();
	
	public void addRequestHandler(RequestHandler requestHandler) {
		requestHandlers.add(0, requestHandler);
	}

	public Response call(Request request) {
		Response res;
		for( Iterator i=requestHandlers.iterator(); i.hasNext(); ) {
			RequestHandler rh = (RequestHandler)i.next();
			res = rh.call(request);
			if( res.getStatus() > 0 ) {
				//System.err.println("  Handled by " + rh.getClass().getName() );
				return res;
			}
		}
		return BaseResponse.RESPONSE_UNHANDLED;
	}
}
