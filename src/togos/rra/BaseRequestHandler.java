package togos.rra;

import togos.mf.MessageIterator;
import togos.mf.MessageIterators;

public abstract class BaseRequestHandler implements RequestHandler, togos.mf.RequestHandler, Getter {
	
	public abstract Response handleRequest( Request req );
	
	protected final static Object getResponseValue( Response res ) {
		if( res.getStatus() == Response.STATUS_NORMAL ) return res.getContent();
		return null;
	}
	
	//// Getter implementation ////
	
	public Object get( String uri ) {
		BaseRequest res = new BaseRequest( "GET", uri );
		return getResponseValue( handleRequest(res) );
	}
	
	//// mf.RequestHandler implementation ////
	
	public Response call(Request req) {
		return handleRequest(req);
	}
	public MessageIterator open(Request req) {
		return MessageIterators.createSingleResultIterator(call(req));
	}
	public void send(Request req) {
		call(req);
	}
}
