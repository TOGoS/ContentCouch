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
	
	protected Expression parseActiveUriExpression( String uri ) {
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
	
	static final class ParsePosition {
		public char[] source;
		public int pos;
		public int end;
		public int lastTokenPos;
		
		public ParsePosition(char[] source) {
			this.source = source;
			this.pos = 0;
			this.end = source.length;
		}
		
		public int current() {
			if( pos >= end ) return -1;
			return source[pos];
		}
	}
	
	protected boolean isWhitespace( int c ) {
		return c == ' ' || c == '\t' || c == '\r' || c == '\n';
	}
	
	protected void skipWhitespace( ParsePosition pp ) {
		for( ; isWhitespace(pp.current()); ++pp.pos );
	}
	
	protected String readToken( ParsePosition pp, boolean stopOnEquals ) {
		skipWhitespace(pp);
		pp.lastTokenPos = pp.pos;
		int c = pp.current();
		if( c == -1 ) return null;
		if( c == '(' ) {
			++pp.pos;
			return "(";
		}
		if( c == ')' ) {
			++pp.pos;
			return ")";
		}
		if( c == '=' && stopOnEquals ) {
			++pp.pos;
			return "=";
		}
		StringBuffer sb = new StringBuffer();
		while( !isWhitespace(c) && c != '(' && c != ')' && c != -1 && (!stopOnEquals || c != '=')) {
			sb.append((char)c);
			++pp.pos;
			c = pp.current();
		}
		return sb.toString();
	}
	
	protected Expression processFunctionExpression( Expression e ) {
		if( e instanceof ResolveUriExpression && ((ResolveUriExpression)e).uri.indexOf(':') == -1 ) {
			return new GetFunctionByNameExpression(((ResolveUriExpression)e).uri);
		}
		return e;
	}
	
	protected Expression parseParenContent( ParsePosition pp ) {
		Expression functionExpression = parseParenExpression(pp);
		if( functionExpression == null ) {
			throw new RuntimeException("Found end of expression where function expected at " + pp.lastTokenPos + " in " + new String(pp.source) );
		}
		functionExpression = processFunctionExpression(functionExpression);
		Map argumentExpressions = new TreeMap();
		String s = readToken(pp, true);
		while( s != null && !")".equals(s) ) {
			if( "(".equals(s) || "=".equals(s) ) {
				throw new RuntimeException("Found '"+s+"' where '=' expected in " + pp.lastTokenPos + " in " + new String(pp.source) );
			}
			String paramName = s;
			s = readToken(pp,true);
			if( !"=".equals(s) ) {
				throw new RuntimeException("Found '"+s+"' where parameter name expected in " + pp.lastTokenPos + " in " + new String(pp.source) );
			}
			Expression paramValueExpression = parseParenExpression(pp);
			argumentExpressions.put(paramName, paramValueExpression);
			s = readToken(pp, true);
		}
		return new CallFunctionExpression( functionExpression, argumentExpressions );
	}
	
	protected Expression parseParenExpression( ParsePosition pp ) {
		String s;
		s = readToken(pp,false);
		if( s == null ) return null;
		if( "(".equals(s) ) {
			return parseParenContent(pp);
		}
		return parseExpression(s);
	}
	
	protected Expression parseParenExpression( String text ) {
		return parseParenExpression( new ParsePosition(text.toCharArray()) );
	}
	
	protected Expression parseExpression( String uri ) {
		if( uri.startsWith(ACTIVE_URI_PREFIX) ) return parseActiveUriExpression(uri);
		if( uri.startsWith("(") ) return parseParenExpression(uri);
		return new ResolveUriExpression(uri);
	}
}
