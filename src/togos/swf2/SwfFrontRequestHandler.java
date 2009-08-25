package togos.swf2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import togos.mf.api.CallHandler;
import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import contentcouch.framework.BaseRequestHandler;

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
			CallHandler rh = (CallHandler)i.next();
			res = rh.call(subReq);
			if( res.getStatus() > 0 ) return res;
		}
		return BaseResponse.RESPONSE_UNHANDLED;
	}
}
