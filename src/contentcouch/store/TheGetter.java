package contentcouch.store;

import java.util.Collections;

import togos.rra.BaseRequest;
import togos.rra.Request;
import togos.rra.RequestHandler;
import togos.rra.Response;
import contentcouch.active.Context;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Directory;

public class TheGetter {
	public static class AbnormalResponseException extends RuntimeException {
		public AbnormalResponseException( String verb, String uri, int status, Object content ) {
			super( verb + " " + uri + " resulted in " + status + (content == null ? "" : ": " + ValueUtil.getString(content)));
		}
	}
	
	public static final String CTXVAR = CcouchNamespace.INTERNAL_NS + "getter"; 
	
	public static RequestHandler globalInstance;
	
	public static RequestHandler getGenericGetter() {
		RequestHandler theGetter = (RequestHandler)Context.get(CTXVAR);
		if( theGetter == null ) {
			theGetter = globalInstance;
		}
		if( theGetter == null ) {
			throw new RuntimeException("No "+CTXVAR+" registered");
		}
		return theGetter;
	}
	
	public static Response handleRequest( Request req ) {
		System.err.println(req.getVerb() + " " + req.getUri());
		return getGenericGetter().handleRequest(req);
	}

	public static Object getResponseValue( Response res, String verb, String uri ) {
		switch( res.getStatus() ) {
		case( Response.STATUS_NORMAL ): return res.getContent();
		default:
			throw new AbnormalResponseException( verb, uri, res.getStatus(), res.getContent() );
		}
	}
	
	public static Object getResponseValue( Response res, String uri ) {
		return getResponseValue(res, "GET", uri );
	}

	public static Object getResponseValue( Response res, Request req ) {
		return getResponseValue(res, req.getVerb(), req.getUri() );
	}

	public static Object get(String uri) {
		Request req = new BaseRequest("GET",uri);
		return getResponseValue(handleRequest(req), req);
	}
	
	public static Object put(String uri, Object obj) {
		BaseRequest putReq = new BaseRequest(Request.VERB_PUT, uri, obj, Collections.EMPTY_MAP);
		return getResponseValue(handleRequest(putReq), putReq);
	}

	public static Directory getDirectory( String uri ) {
		return (Directory)get("active:contentcouch.directoryize+operand@" + UriUtil.uriEncode(uri));
	}
}
