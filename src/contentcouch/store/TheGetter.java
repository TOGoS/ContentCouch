package contentcouch.store;

import togos.rra.BaseRequest;
import togos.rra.Request;
import togos.rra.RequestHandler;
import togos.rra.Response;
import contentcouch.active.Context;

public class TheGetter {
	public static RequestHandler globalInstance;
	
	public static RequestHandler getGenericGetter() {
		RequestHandler theGetter = (RequestHandler)Context.getInstance().get(Context.GENERIC_GETTER_VAR);
		if( theGetter == null ) {
			theGetter = globalInstance;
		}
		if( theGetter == null ) {
			throw new RuntimeException("No "+Context.GENERIC_GETTER_VAR+" registered");
		}
		return theGetter;
	}
	
	public static Object get(String uri) {
		Response res = handleRequest(new BaseRequest("GET",uri));
		if( res.getStatus() == Response.STATUS_NORMAL ) return res.getContent();
		return null;
	}
	
	public static Response handleRequest( Request req ) {
		//System.err.println(req.getVerb() + " " + req.getUri());
		return getGenericGetter().handleRequest(req);
	}
}
