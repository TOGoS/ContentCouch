package contentcouch.framework;

import togos.mf.api.Request;
import togos.mf.api.Callable;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;

public abstract class BaseRequestHandler implements Callable {
	
	public abstract Response call( Request req );
	
	protected final static Object getResponseValue( Response res ) {
		if( res.getStatus() == ResponseCodes.NORMAL ) return res.getContent();
		return null;
	}
	
	//// Getter implementation ////
	
	public Object get( String uri ) {
		BaseRequest res = new BaseRequest( "GET", uri );
		return getResponseValue( call(res) );
	}
}
