package contentcouch.active;

import java.util.Map;

import contentcouch.store.Getter;

public class ResolveUriExpression implements Expression {
	public static final String URI_RESOLVER_VARNAME = "ccouch:uri-resolver";

	String uri;
	
	public ResolveUriExpression( String uri ) {
		this.uri = uri;
	}
	
	public String getUri() {
		return uri;
	}
	
	public String toString() {
		return uri;
	}
	
	public Object eval( Map context ) {
		Getter uriResolverObj = (Getter)context.get(URI_RESOLVER_VARNAME);
		if( uriResolverObj == null ) {
			throw new RuntimeException("No ccouch:uri-resolver registered");
		}
		return uriResolverObj.get(uri);
	}
}
