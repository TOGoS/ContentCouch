package togos.swf2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import togos.mf.api.CallHandler;
import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import togos.mf.value.Arguments;
import contentcouch.framework.BaseRequestHandler;

public class SwfFrontRequestHandler extends BaseRequestHandler {
	protected Map components = new HashMap();

	public SwfFrontRequestHandler() {
	}
	
	public void putComponent( String name, Object c ) {
		components.put(name,c);
	}
	
	public String getExternalUri( Request req, String internalUri ) {
		if( internalUri == null ) return internalUri;
		if( internalUri.startsWith(SwfNamespace.SERVLET_PATH_URI_PREFIX) ) {
			return "/es2" +internalUri.substring(SwfNamespace.SERVLET_PATH_URI_PREFIX.length());
			/*
			TODO: fancy ../ optimizations
			
			int qidx = internalUri.indexOf('?');
			int pidx = internalUri.indexOf('#');
			int end = -1;
			if( qidx > -1 && (end == -1 || qidx > end) ) end = qidx;
			if( pidx > -1 && (end == -1 || pidx > end) ) end = pidx;
			String path = (end == -1) ? internalUri : internalUri.substring(0,end);
			*/
		}
		return internalUri;
	}
	
	public String getExternalComponentUri( Request req, Component component, Arguments args ) {
		return getExternalUri( req, component.getUriFor(args) );
	}
	
	public Response call( Request request ) {
		BaseRequest subReq = new BaseRequest(request);
		subReq.putContextVar(SwfNamespace.COMPONENTS, components);
		subReq.putContextVar(SwfNamespace.FRONT, this);
		
		Response res;
		for( Iterator i=components.values().iterator(); i.hasNext(); ) {
			CallHandler rh = (CallHandler)i.next();
			res = rh.call(subReq);
			if( res.getStatus() > 0 ) return res;
		}
		return BaseResponse.RESPONSE_UNHANDLED;
	}
}
