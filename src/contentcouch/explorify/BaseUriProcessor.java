package contentcouch.explorify;

import contentcouch.active.Context;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;

public class BaseUriProcessor implements UriProcessor {
	public static final String CTXVAR = CcouchNamespace.INTERNAL_NS + "uriProcessor";
	public static final BaseUriProcessor BASEINSTANCE = new BaseUriProcessor( null, false );

	public static UriProcessor getInstance() {
		UriProcessor proc = (UriProcessor)Context.get(CTXVAR);
		if( proc == null ) {
			System.err.println("No " + CTXVAR);
		}
		return ( proc == null ) ? BASEINSTANCE : proc;
	}
	
	public static void setInstance( UriProcessor proc ) {
		Context.put(CTXVAR, proc);
	}
	
	public static void push( UriProcessor proc ) {
		Context.push( CTXVAR, proc );
	}
	
	public static Object pop() {
		return Context.pop( CTXVAR );
	}
	
	protected UriProcessor postProcessor;
	protected boolean shouldRewriteRelativeUris;
	
	public BaseUriProcessor( UriProcessor postProcessor, boolean shouldRewriteRelativeUris ) {
		this.postProcessor = postProcessor;
		this.shouldRewriteRelativeUris = shouldRewriteRelativeUris;
	}
	
	public String processUri(String uri) {
		return ( postProcessor == null ) ? uri : postProcessor.processUri( uri );
	}
	
	public String processRelativeUri(String baseUri, String relativeUri) {
		if( !shouldRewriteRelativeUris && PathUtil.isRelative(relativeUri) ) {
			return relativeUri;
		} else {
			return processUri( PathUtil.appendPath(baseUri, relativeUri) );
		}
	}
}
