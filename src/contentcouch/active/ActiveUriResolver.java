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
		if( uri.startsWith(ACTIVE_URI_PREFIX) || uri.startsWith("(") || uri.startsWith("\"") ) {
			Expression e = parseExpression( uri );
			//Map context = Context.getInstance();
			//context.put(Context.GENERIC_GETTER_VAR, getter);
			//context.put(GetFunctionByNameExpression.FUNCTION_MAP_VARNAME, this.namedActiveFunctions);
			return e.eval();
		}
		return null;
	}
	
	protected Expression parseActiveUriExpression( String uri ) {
		String[] parts = uri.substring(ACTIVE_URI_PREFIX.length()).split("\\+");
		String funcName = UriUtil.uriDecode(parts[0]);
		TreeMap argumentExpressions = new TreeMap();
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
		
		public final int current() {
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
		if( c == '"' ) {
			StringBuffer b = new StringBuffer();
			b.append('"');
			++pp.pos;
			c = pp.current();
			while( c != '"' && c != -1 ) {
				if( c == '\\' ) {
					++pp.pos;
					c = pp.current();
					switch( c ) {
					case( 't' ): c = '\t'; break;
					case( 'r' ): c = '\r'; break;
					case( 'n' ): c = '\n'; break;
					}
				}
				b.append( (char)c );
				++pp.pos;
				c = pp.current();
			}
			b.append('"');
			++pp.pos;
			return b.toString();
		}
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
		if( e instanceof Bareword ) {
			return new GetFunctionByNameExpression(e.toString());
		}
		return e;
	}
	
	protected Expression parseParenContent( ParsePosition pp ) {
		Expression functionExpression = parseParenExpression(pp, false);
		if( functionExpression == null ) {
			throw new RuntimeException("Found end of expression where function expected at " + pp.lastTokenPos + " in " + new String(pp.source) );
		}
		functionExpression = processFunctionExpression(functionExpression);
		TreeMap argumentExpressions = new TreeMap();

		Expression e = parseParenExpression(pp, true);
		int operandCounter = 0;
		while( e != null ) {
			Expression n = parseParenExpression(pp, true);
			if( n instanceof Bareword && "=".equals(n.toString()) ) {
				if( !(e instanceof Bareword) ) {
					throw new RuntimeException("Bareword expected before '=', but found " + e.getClass().getName());
				}
				Expression v = parseParenExpression(pp, false);
				if( v == null ) {
					throw new RuntimeException("Reached end of expression where value expected");
				}
				argumentExpressions.put(e.toString(), v);
				e = parseParenExpression(pp, true);
			} else {
				int oc = operandCounter++;
				if( oc == 0 ) {
					argumentExpressions.put("operand", e);
				} else {
					// TODO: Format as 0001 so that it is sorted properly when >= 10
					argumentExpressions.put("operand" + oc, e);
				}
				e = n;
			}
		}
		return new CallFunctionExpression( functionExpression, argumentExpressions );
	}
	
	protected Expression parseParenExpression( ParsePosition pp, boolean stopOnEquals ) {
		String s;
		s = readToken(pp,stopOnEquals);
		if( s == null ) return null;
		if( ")".equals(s) ) return null;
		if( "(".equals(s) ) return parseParenContent(pp);
		return parseExpression(s);
	}
	
	protected Expression parseParenExpression( String text ) {
		return parseParenExpression( new ParsePosition(text.toCharArray()), false );
	}
	
	protected Expression parseExpression( String uri ) {
		if( uri.startsWith(ACTIVE_URI_PREFIX) ) return parseActiveUriExpression(uri);
		if( uri.startsWith("(") ) return parseParenExpression(uri);
		if( uri.startsWith("\"") ) return new ValueExpression(uri.substring(1,uri.length()-1));
		if( uri.indexOf(":") == -1 ) {
			return new Bareword(uri);
		} else {
			return new ResolveUriExpression(uri);
		}
	}
}
