package contentcouch.active;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import contentcouch.misc.UriUtil;
import contentcouch.store.Getter;

public class ActiveUriResolver implements Getter {
	public static final String ACTIVE_URI_PREFIX = "active:";
	public static final char[] HEXCHARS = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	public Getter getter;
	public Map namedActiveFunctions = new HashMap();
	
	public ActiveUriResolver( Getter getter ) {
		this.getter = getter;
	}
	
	public Object get( String uri ) {
		if( !uri.startsWith(ACTIVE_URI_PREFIX) ) return null;
		
		Expression e = parseExpression( uri );
		Map context = new HashMap();
		context.put(ResolveUriExpression.URI_RESOLVER_VARNAME, getter);
		context.put(GetFunctionByNameExpression.FUNCTION_MAP_VARNAME, this.namedActiveFunctions);
		return e.eval(context);
	}
	
	protected Expression parseExpression( String uri ) {
		if( !uri.startsWith(ACTIVE_URI_PREFIX) ) return new ResolveUriExpression(uri);
		
		String[] parts = uri.substring(ACTIVE_URI_PREFIX.length()).split("\\+");
		String funcName = UriUtil.uriDecode(parts[0]);
		Map argumentExpressions = new TreeMap();
		for( int i=1; i<parts.length; ++i ) {
			String[] kv = parts[i].split("@",2);
			argumentExpressions.put(kv[0], parseExpression(UriUtil.uriDecode(kv[1])));
		}
		Expression funcExpression = (funcName.indexOf(':') == -1) ? new GetFunctionByNameExpression(funcName) : parseExpression(funcName);
		return new CallFunctionExpression(funcExpression, argumentExpressions);
	}
}
