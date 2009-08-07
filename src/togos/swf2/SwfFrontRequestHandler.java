package togos.swf2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import contentcouch.framework.BaseRequestHandler;

import togos.mf.Request;
import togos.mf.RequestHandler;
import togos.mf.Response;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;

public class SwfFrontRequestHandler extends BaseRequestHandler {
	protected Map components = new HashMap();

	public SwfFrontRequestHandler() {
	}
	
	public void putComponent( String name, Component c ) {
		components.put(name,c);
	}

	public Response call( Request request ) {
		BaseRequest subReq = new BaseRequest(request);
		subReq.putMetadata(SwfNamespace.COMPONENTS, components);
		
		Response res;
		for( Iterator i=components.values().iterator(); i.hasNext(); ) {
			RequestHandler rh = (RequestHandler)i.next();
			res = rh.call(subReq);
			if( res.getStatus() > 0 ) return res;
		}
		return BaseResponse.RESPONSE_UNHANDLED;
	}
}
