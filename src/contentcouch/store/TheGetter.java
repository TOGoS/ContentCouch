package contentcouch.store;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import togos.mf.api.CallHandler;
import togos.mf.api.ContentAndMetadata;
import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import togos.mf.value.Blob;
import togos.swf2.SwfNamespace;
import contentcouch.active.ActiveRequestHandler;
import contentcouch.active.DataUriResolver;
import contentcouch.active.expression.Expression;
import contentcouch.app.Log;
import contentcouch.context.Context;
import contentcouch.framework.MultiRequestHandler;
import contentcouch.misc.ContextVarRequestHandler;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class TheGetter {
	public static class AbnormalResponseException extends RuntimeException {
		public AbnormalResponseException( String verb, String uri, int status, Object content ) {
			super( verb + " " + uri + " resulted in " + status + (content == null ? "" : ": " + ValueUtil.getString(content)));
		}
	}
	
	public static BaseRequest createRequest( String verb, String uri ) {
		BaseRequest req = new BaseRequest( verb, uri );
		req.metadata = Context.getInstance();
		return req;
	}
	
	public static BaseRequest createRequest( String verb, String uri, Object content, Map contentMetadata ) {
		BaseRequest req = new BaseRequest( verb, uri, content, contentMetadata );
		req.metadata = Context.getInstance();
		return req;
	}
	
	public static final String CTXVAR = SwfNamespace.CTX_GETTER; 
	
	public static CallHandler globalInstance;
	
	public static CallHandler getGenericGetter(Request req) {
		CallHandler theGetter = (CallHandler)req.getMetadata().get(CTXVAR);
		if( theGetter == null ) {
			theGetter = globalInstance;
		}
		if( theGetter == null ) {
			throw new RuntimeException("No "+CTXVAR+" registered");
		}
		return theGetter;
	}
	
	protected static String describeContent(Object content) {
		String desc = content.getClass().getName();
		if( content instanceof Blob ) {
			desc += " (" + ((Blob)content).getLength() + " bytes)";
		}
		return desc;
	}
	
	public static void initializeBasicCallHandlers( MultiRequestHandler mrh ) {
		mrh.addRequestHandler(new ParseRdfRequestHandler());
		mrh.addRequestHandler(new ContextVarRequestHandler());
		mrh.addRequestHandler(new ActiveRequestHandler());
		mrh.addRequestHandler(new DataUriResolver());
		mrh.addRequestHandler(new ParseRdfRequestHandler());
		mrh.addRequestHandler(new ContextVarRequestHandler());

	}
	
	public static CallHandler getBasicCallHandler() {
		MultiRequestHandler mrh = new MultiRequestHandler();
		initializeBasicCallHandlers(mrh);
		return mrh;
	}
	
	public static Response call( Request req ) {
		Log.log(Log.EVENT_REQUEST_SUBMITTED, req.getVerb(), req.getResourceName(), (req.getContent() == null ? "" : describeContent(req.getContent())) );
		Map oldContext = Context.getInstance();
		try {
			// This should be done at the callee end, but since that's
			// not as centralized as the caller end, I do it here for now,
			// since the way this thing is currently architected it should
			// work most of the time:
			Context.setThreadLocalInstance( req.getMetadata() );
			return getGenericGetter(req).call(req);
		} finally {
			Context.setThreadLocalInstance( oldContext );
		}
	}
	
	public static Response callAndThrowIfNonNormalResponse( Request req ) {
		return throwIfNonNormalResponse( call(req), req );
	}
	
	public static Response throwIfNonNormalResponse( Response res, String verb, String resourceName ) {
		switch( res.getStatus() ) {
		case( ResponseCodes.RESPONSE_NORMAL ): return res;
		default:
			throw new AbnormalResponseException( verb, resourceName, res.getStatus(), res.getContent() );
		}
	}
	
	public static Response throwIfNonNormalResponse( Response res, Request req ) {
		return throwIfNonNormalResponse( res, req.getVerb(), req.getResourceName() );
	}
	
	public static Object getResponseValue( Response res, String verb, String uri ) {
		switch( res.getStatus() ) {
		case( ResponseCodes.RESPONSE_NORMAL ): return res.getContent();
		case( ResponseCodes.RESPONSE_DOESNOTEXIST ): return null;
		default:
			throw new AbnormalResponseException( verb, uri, res.getStatus(), res.getContent() );
		}
	}
	
	public static Object getResponseValue( Response res, String uri ) {
		return getResponseValue(res, "GET", uri );
	}

	public static Object getResponseValue( Response res, Request req ) {
		return getResponseValue(res, req.getVerb(), req.getResourceName() );
	}
	
	public static final String getResponseErrorSummary( Response res ) {
		return res.getStatus() + ": " + ValueUtil.getString(res.getContent()); 
	}

	public static Object get(String uri) {
		Request req = createRequest("GET",uri);
		return getResponseValue(call(req), req);
	}
	
	public static Object put(String uri, Object obj) {
		BaseRequest putReq = createRequest(RequestVerbs.VERB_PUT, uri, obj, Collections.EMPTY_MAP);
		return getResponseValue(call(putReq), putReq);
	}

	public static Directory getDirectory( String uri ) {
		return (Directory)get("active:contentcouch.directoryize+operand@" + UriUtil.uriEncode(uri));
	}
	
	public static String identify( Object content, Map contentMetadata ) {
		BaseRequest idReq = createRequest(RequestVerbs.VERB_POST, "x-ccouch-repo:identify", content, contentMetadata );
		return ValueUtil.getString(TheGetter.getResponseValue(TheGetter.call(idReq), idReq.uri));
	}
	
	public static String reference( Object o, boolean allowFileUris, boolean allowGenerateContentUris ) {
		if( o instanceof Ref ) {
			return ((Ref)o).getTargetUri();
		}
		if( allowFileUris && o instanceof File ) {
			File f = (File)o;
			return PathUtil.maybeNormalizeFileUri(f.isDirectory() ? f.getAbsolutePath() + "/" : f.getAbsolutePath());
		}
		if( allowGenerateContentUris ) {
			identify(o, Collections.EMPTY_MAP);
		}
		throw new RuntimeException("Don't know how to reference " + (o == null ? "null" : "a " + o.getClass().getName()));
	}
	
	public static ContentAndMetadata dereferenceAsResponse( Object content, Map contentMetadata ) {
		if( content instanceof Ref ) {
			return callAndThrowIfNonNormalResponse(createRequest( RequestVerbs.VERB_GET, ((Ref)content).getTargetUri()));
		}
		if( content instanceof Expression ) {
			BaseRequest subReq = createRequest(RequestVerbs.VERB_GET, "(no URI passed to expression.eval)");
			return throwIfNonNormalResponse(((Expression)content).eval(subReq), subReq);
		}
		BaseResponse res = new BaseResponse( ResponseCodes.RESPONSE_NORMAL, content );
		res.contentMetadata = contentMetadata;
		return res;
	}
	
	public static Object dereference( Object o ) {
		return dereferenceAsResponse( o, Collections.EMPTY_MAP ).getContent();
	}
}