package contentcouch.active;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.active.expression.FunctionCallExpression;
import contentcouch.active.expression.Expression;
import contentcouch.active.expression.FunctionByNameExpression;
import contentcouch.path.PathSimplifiableActiveFunction;
import contentcouch.path.PathSimplifiableExpression;


public abstract class BaseActiveFunction implements ActiveFunction, PathSimplifiableActiveFunction {
	public boolean isConstant( Map argumentExpressions ) {
		return false;
	}

	protected static Response getArgumentResponse( Request req, Map argumentExpressions, String name ) {
		Expression e = (Expression)argumentExpressions.get(name);
		if( e == null ) return new BaseResponse(ResponseCodes.DOES_NOT_EXIST, "Missing argument " + name, "text/plain");
		return e.eval(req);
	}
	
	protected static Object getArgumentValue( Request req, Map argumentExpressions, String name, Object defaultValue ) {
		Expression e = (Expression)argumentExpressions.get(name);
		if( e == null ) return defaultValue;
		Response res = e.eval(req);
		if( res.getStatus() == ResponseCodes.NORMAL ) {
			if( res.getContent() == null ) return defaultValue;
			return res.getContent();
		}
		throw new RuntimeException( "Couldn't load " + e.toString() + ": " + res.getStatus() + ": " + res.getContent());
	}
	
	protected static List getPositionalArgumentExpressions( Map argumentExpressions, String startingWith ) {
		List l = new ArrayList();
		for( Iterator i=argumentExpressions.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			String s = (String)e.getKey();
			if( s.startsWith(startingWith) ) {
				String numericPart = s.substring(startingWith.length());
				int pos;
				if( numericPart.length() == 0 ) {
					pos = 0;
				} else {
					pos = Integer.parseInt(numericPart);
				}
				if( pos > 128 ) throw new RuntimeException("Too many positional arguments");
				while( l.size() < pos ) l.add(null);
				l.add(pos, e.getValue());
			}
		}
		return l;
	}
	
	protected static List getPositionalArgumentExpressions( Map argumentExpressions ) {
		return getPositionalArgumentExpressions( argumentExpressions, "operand" );
	}

	protected static List getArgumentExpressionValues( Request req, List argumentExpressions ) {
		List values = new ArrayList();
		for( Iterator i=argumentExpressions.iterator(); i.hasNext(); ) {
			Expression exp = (Expression)i.next();
			Response res = exp != null ? exp.eval(req) : null;
			if( res != null ) {
				if( res != null && res.getStatus() == ResponseCodes.NORMAL ) {
					values.add( res.getContent() );
				} else {
					throw new RuntimeException( "Couldn't load " + exp.toString() + ": " + res.getStatus() + ": " + res.getContent());
				}
			} else {
				values.add( null );
			}
		}
		return values;
	}
	
	protected static List getPositionalArgumentValues( Request req, Map argumentExpressions ) {
		return getArgumentExpressionValues( req, getPositionalArgumentExpressions( argumentExpressions ) );
	}

	protected static List getPositionalArgumentValues( Request req, Map argumentExpressions, String startingWith ) {
		return getArgumentExpressionValues( req, getPositionalArgumentExpressions( argumentExpressions, startingWith ) );
	}
	
	//// Path simplification ////
	
	protected String getPathArgumentName() {
		return null;
	}
	
	public Expression appendPath(Expression funcExpression, Map argumentExpressions, String path) {
		String pathArgName = getPathArgumentName();
		if( pathArgName == null ) return null;

		Expression pathExpression = (Expression)argumentExpressions.get(pathArgName);
		if( pathExpression == null || !(pathExpression instanceof PathSimplifiableExpression) ) return null;		

		pathExpression = ((PathSimplifiableExpression)pathExpression).appendPath(path);
		if( pathExpression == null ) return null;
		
		TreeMap newArgs = new TreeMap(argumentExpressions);
		newArgs.put(pathArgName, pathExpression);
		
		return new FunctionCallExpression(funcExpression, newArgs);
	}
	
	public Expression simplify(Map argumentExpressions) {
		return null;
	}
	
	////
	
	protected String camelCaseToDashed(String camelCase) {
		String dashed = "";
		for( int i=0; i<camelCase.length(); ++i ) {
			char c = camelCase.charAt(i);
			if( c >= 'A' && c <= 'Z' ) {
				dashed += "-";
				c += ('a'-'A');
			}
			dashed += c;
		}
		return dashed;
	}
	
	public String getFunctionName() {
		String cName = this.getClass().getName();
		if( cName == null ) return null;
		String[] parts = cName.split("\\.");
		if( parts.length < 2 ) return null;
		String name = "";
		for( int i=0; i<parts.length-2; ++i ) {
			name += parts[i] + ".";
		}
		name += camelCaseToDashed(parts[parts.length-1]);
		return name;
	}
	
	protected FunctionCallExpression toCallExpression( Map argumentExpressions ) {
		String name = getFunctionName();
		if( name == null ) throw new RuntimeException("No function name for " + getClass().getName());
		return new FunctionCallExpression(new FunctionByNameExpression(name), argumentExpressions );
	}
}
