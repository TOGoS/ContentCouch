package contentcouch.store;

import togos.rra.BaseRequest;
import togos.rra.Request;
import togos.rra.RequestHandler;
import togos.rra.Response;
import contentcouch.active.Context;
import contentcouch.misc.UriUtil;
import contentcouch.value.Directory;

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
		return getResponseValue(handleRequest(new BaseRequest("GET",uri)), uri);
	}
	
	public static Object getResponseValue( Response res, String uri ) {
		if( res.getStatus() == Response.STATUS_NORMAL ) return res.getContent();
		throw new RuntimeException( "Couldn't load " + uri + ": " + res.getStatus() + ": " + res.getContent());
	}
	
	public static Response handleRequest( Request req ) {
		System.err.println(req.getVerb() + " " + req.getUri());
		return getGenericGetter().handleRequest(req);
	}
	
	public static Directory getDirectory( String uri ) {
		return (Directory)get("active:contentcouch.directoryize+operand@" + UriUtil.uriEncode(uri));
	}
}
