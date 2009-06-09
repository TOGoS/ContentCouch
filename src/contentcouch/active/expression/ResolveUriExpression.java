package contentcouch.active.expression;

import togos.rra.BaseRequest;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.active.Context;
import contentcouch.misc.UriUtil;
import contentcouch.path.PathSimplifiableExpression;
import contentcouch.path.PathUtil;
import contentcouch.store.TheGetter;

public class ResolveUriExpression implements Expression, PathSimplifiableExpression {
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
		req.contextVars = Context.getInstance();
		return TheGetter.handleRequest(req);
	}
	
	public String toUri() {
		return uri;
	}
	
	public boolean isConstant() {
		if( uri.startsWith("data:") ) return true;
		if( uri.startsWith("urn:sha1:") ) return true;
		return false;
	}
	
	public Expression appendPath(String path) {
		if( PathUtil.isHierarchicalUri(uri) ) {
			return new ResolveUriExpression(PathUtil.appendHierarchicalPath(uri, path, true));
		} else {
			return null;
		}
	}
	
	public Expression simplify() {
		return this;
	}
}
