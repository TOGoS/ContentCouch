package contentcouch.active;

import contentcouch.misc.UriUtil;
import contentcouch.store.Getter;

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
	
	public Object eval() {
		Getter uriResolverObj = (Getter)Context.getInstance().get(Context.URI_RESOLVER_VARNAME);
		if( uriResolverObj == null ) {
			throw new RuntimeException("No ccouch:uri-resolver registered");
		}
		return uriResolverObj.get(uri);
	}
	
	public String toUri() {
		return uri;
	}
}
