package contentcouch.active.expression;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import togos.rra.Response;
import contentcouch.active.ActiveFunction;
import contentcouch.active.ActiveUtil;
import contentcouch.misc.UriUtil;
import contentcouch.path.PathSimplifiableActiveFunction;
import contentcouch.path.PathSimplifiableExpression;

public class CallFunctionExpression implements Expression, PathSimplifiableExpression {
	Expression funcExpression;
	SortedMap argumentExpressions;
	
	public CallFunctionExpression( Expression funcExpression, SortedMap argumentExpressions ) {
		this.funcExpression = funcExpression;
		this.argumentExpressions = argumentExpressions;
	}
	
	public String toString() {
		String res = "(";
		if( funcExpression instanceof GetFunctionByNameExpression ) {
			res += UriUtil.uriEncode(((GetFunctionByNameExpression)funcExpression).funcName);
		} else {
			res += funcExpression.toString();
		}
		for( Iterator i=argumentExpressions.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			res += " " + UriUtil.uriEncode((String)e.getKey()) + "=" + e.getValue().toString(); 
		}
		res += ")";
		return res;
	}
	
	public String toUri() {
		String s = "active:";
		if( funcExpression instanceof GetFunctionByNameExpression ) {
			s += UriUtil.uriEncode(((GetFunctionByNameExpression)funcExpression).funcName);
		} else {
			s += UriUtil.uriEncode(funcExpression.toString()); 
		}
		for( Iterator i=argumentExpressions.entrySet().iterator(); i.hasNext(); ) {
			s += "+";
			Map.Entry e = (Map.Entry)i.next();
			String k = (String)e.getKey();
			Expression exp = (Expression)e.getValue();
			s += k + "@" + UriUtil.uriEncode(exp.toUri());
		}
		return s;
	}

	public Response eval() {
		Response fRes = funcExpression.eval();
		if( fRes.getStatus() != Response.STATUS_NORMAL ) throw new RuntimeException("Could not load function " + funcExpression.toString() + ": " + fRes.getStatus() + ": " + fRes.getContent() );
		if( !(fRes.getContent() instanceof ActiveFunction) ) throw new RuntimeException( "Object returned by " + funcExpression.toString() + " is not an ActiveFunction");
		return ((ActiveFunction)fRes.getContent()).call(argumentExpressions);
	}
	
	public boolean isConstant() {
		return false;
	}

	protected ActiveFunction getStaticActiveFunction( Expression funcExpression ) {
		if( funcExpression.isConstant() ) {
			Response fRes = funcExpression.eval();
			if( fRes.getStatus() != Response.STATUS_NORMAL ) throw new RuntimeException("Could not load function " + funcExpression.toString() + ": " + fRes.getStatus() + ": " + fRes.getContent() );
			if( !(fRes.getContent() instanceof ActiveFunction) ) throw new RuntimeException( "Object returned by " + funcExpression.toString() + " is not an ActiveFunction");
			return (ActiveFunction)fRes.getContent();
		}
		return null;
	}
	
	public Expression appendPath(String path) {
		ActiveFunction f = getStaticActiveFunction(funcExpression);
		if( f != null && f instanceof PathSimplifiableActiveFunction ) {
			return ((PathSimplifiableActiveFunction)f).appendPath(funcExpression, argumentExpressions, path);
		}
		return null;
	}

	public Expression simplify() {
		boolean simplified = false;
		Expression simplifiedFuncExpression = ActiveUtil.simplify(funcExpression);
		if( simplifiedFuncExpression != funcExpression ) simplified = true;
		TreeMap simplifiedArgumentExpressions = new TreeMap();
		for( Iterator i=argumentExpressions.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			Expression simplifiedArgExpression = ActiveUtil.simplify((Expression)e.getValue());
			if( simplifiedArgExpression != e.getValue() ) simplified = true;
			simplifiedArgumentExpressions.put(e.getKey(), simplifiedArgExpression);
		}
		
		ActiveFunction f = getStaticActiveFunction(simplifiedFuncExpression);
		if( f != null && f instanceof PathSimplifiableActiveFunction ) {
			Expression e = ((PathSimplifiableActiveFunction)f).simplify(simplifiedArgumentExpressions);
			if( e != null ) return e;			
		}
		if( simplified ) {
			return new CallFunctionExpression(simplifiedFuncExpression, simplifiedArgumentExpressions);
		} else {
			return this;
		}
	}
}
