package togos.rra;

public abstract class BaseRequestHandler implements RequestHandler, Getter {
	
	public abstract Response handleRequest( Request req );
	
	protected final static Object getResponseValue( Response res ) {
		if( res.getStatus() == Response.STATUS_UNHANDLED ) return null;
		return res.getContent();
	}
	
	public Object get( String uri ) {
		BaseRequest res = new BaseRequest( "GET", uri );
		return getResponseValue( handleRequest(res) );
	}
}
