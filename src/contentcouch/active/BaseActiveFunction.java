package contentcouch.active;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.expression.CallFunctionExpression;
import contentcouch.active.expression.Expression;
import contentcouch.path.PathSimplifiableActiveFunction;
import contentcouch.path.PathSimplifiableExpression;


public abstract class BaseActiveFunction implements ActiveFunction, PathSimplifiableActiveFunction {
	protected Response getArgumentResponse( Map argumentExpressions, String name ) {
		Expression e = (Expression)argumentExpressions.get(name);
		if( e == null ) return new BaseResponse(Response.STATUS_DOESNOTEXIST, "Missing argument " + name, "text/plain");
		return e.eval();
	}
	
	protected Object getArgumentValue( Map argumentExpressions, String name, Object defaultValue ) {
		Expression e = (Expression)argumentExpressions.get(name);
		if( e == null ) return defaultValue;
		Response res = e.eval();
		if( res.getStatus() == Response.STATUS_NORMAL ) return res.getContent();
		throw new RuntimeException( "Couldn't load " + e.toString() + ": " + res.getStatus() + ": " + res.getContent());
	}
	
	protected List getPositionalArgumentExpressions( Map argumentExpressions, String startingWith ) {
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
	
	protected List getPositionalArgumentExpressions( Map argumentExpressions ) {
		return getPositionalArgumentExpressions( argumentExpressions, "operand" );
	}

	protected List getArgumentExpressionValues( List argumentExpressions ) {
		List values = new ArrayList();
		for( Iterator i=argumentExpressions.iterator(); i.hasNext(); ) {
			Expression exp = (Expression)i.next();
			Response res = exp != null ? exp.eval() : null;
			if( res != null ) {
				if( res != null && res.getStatus() == Response.STATUS_NORMAL ) {
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
	
	protected List getPositionalArgumentValues( Map argumentExpressions ) {
		return getArgumentExpressionValues( getPositionalArgumentExpressions( argumentExpressions ) );
	}

	protected List getPositionalArgumentValues( Map argumentExpressions, String startingWith ) {
		return getArgumentExpressionValues( getPositionalArgumentExpressions( argumentExpressions, startingWith ) );
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
		
		return new CallFunctionExpression(funcExpression, newArgs);
	}
	
	public Expression simplify(Map argumentExpressions) {
		return null;
	}
}
