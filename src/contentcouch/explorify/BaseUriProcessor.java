package contentcouch.explorify;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.base.BaseRequest;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;

public class BaseUriProcessor implements UriProcessor {
	public static final String CTXVAR = CcouchNamespace.INTERNAL_NS + "uriProcessor";
	public static final BaseUriProcessor BASEINSTANCE = new BaseUriProcessor( null, false );

	public static UriProcessor getInstance( Map ctx, String whichOne) {
		UriProcessor proc = (UriProcessor)ctx.get(CTXVAR + "/" + whichOne);
		return ( proc == null ) ? BASEINSTANCE : proc;
	}

	public static UriProcessor getInstance( Request req, String whichOne) {
		return getInstance( req.getContextVars(), whichOne );
	}
	
	public static void setInstance( BaseRequest req, String whichOne, UriProcessor proc ) {
		req.putContextVar( CTXVAR + "/" + whichOne, proc);
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
