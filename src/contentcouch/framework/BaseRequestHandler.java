package contentcouch.framework;

import togos.mf.api.Request;
import togos.mf.api.RequestHandler;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.api.ResponseSession;
import togos.mf.base.BaseRequest;
import togos.mf.base.ResponseSessions;

public abstract class BaseRequestHandler implements RequestHandler {
	
	public abstract Response call( Request req );
	
	protected final static Object getResponseValue( Response res ) {
		if( res.getStatus() == ResponseCodes.RESPONSE_NORMAL ) return res.getContent();
		return null;
	}
	
	//// Getter implementation ////
	
	public Object get( String uri ) {
		BaseRequest res = new BaseRequest( "GET", uri );
		return getResponseValue( call(res) );
	}
	
	//// mf.RequestHandler implementation ////
	
	public ResponseSession open(Request req) {
		return ResponseSessions.createSingleResultSession(call(req));
	}
	public boolean send(Request req) {
		return call(req) != null;
	}
}
