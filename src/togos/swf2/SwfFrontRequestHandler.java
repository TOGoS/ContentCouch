package togos.swf2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import togos.rra.BaseRequest;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.RequestHandler;
import togos.rra.Response;

public class SwfFrontRequestHandler implements RequestHandler {
	protected Map components = new HashMap();

	public SwfFrontRequestHandler() {
	}
	
	public void putComponent( String name, Component c ) {
		components.put(name,c);
	}

	public Response handleRequest( Request request ) {
		System.err.println( request.getUri() + " <- uri");
		BaseRequest subReq = new BaseRequest(request);
		subReq.putContextVar(SwfNamespace.COMPONENTS, components);
		Response res;
		for( Iterator i=components.values().iterator(); i.hasNext(); ) {
			RequestHandler rh = (RequestHandler)i.next();
			res = rh.handleRequest(subReq);
			if( res.getStatus() > 0 ) return res;
		}
		return BaseResponse.RESPONSE_UNHANDLED;
	}
}
