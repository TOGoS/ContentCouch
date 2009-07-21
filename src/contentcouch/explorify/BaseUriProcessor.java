package contentcouch.explorify;

import contentcouch.active.Context;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;

public class BaseUriProcessor implements UriProcessor {
	public static final String CTXVAR = CcouchNamespace.INTERNAL_NS + "uriProcessor";
	public static final BaseUriProcessor BASEINSTANCE = new BaseUriProcessor( null, false );

	public static UriProcessor getInstance(String whichOne) {
		UriProcessor proc = (UriProcessor)Context.get(CTXVAR + "/" + whichOne);
		return ( proc == null ) ? BASEINSTANCE : proc;
	}
	
	public static void setInstance( String whichOne, UriProcessor proc ) {
		Context.put( CTXVAR + "/" + whichOne, proc);
	}
	
	public static void push( String whichOne, UriProcessor proc ) {
		Context.push( CTXVAR + "/" + whichOne, proc );
	}
	
	public static Object pop( String whichOne ) {
		return Context.pop( CTXVAR + "/" + whichOne );
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
