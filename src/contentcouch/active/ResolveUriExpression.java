package contentcouch.active;

import togos.rra.BaseRequest;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.misc.UriUtil;
import contentcouch.store.TheGetter;

public class ResolveUriExpression implements Expression {
	String uri;
	
	public ResolveUriExpression( String uri ) {
		this.uri = uri;
	}
	
	public String getUri() {
		return uri;
	}
	
	public String toString() {
		return UriUtil.sanitizeUri(uri);
	}
	
	public Response eval() {
		BaseRequest req = new BaseRequest( Request.VERB_GET, uri );
		return TheGetter.handleRequest(req);
	}
	
	public String toUri() {
		return uri;
	}
}
