package contentcouch.active;

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
	
	public Object eval() {
		return TheGetter.get(uri);
	}
	
	public String toUri() {
		return uri;
	}
}
