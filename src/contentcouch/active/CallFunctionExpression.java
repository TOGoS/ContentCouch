package contentcouch.active;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

import contentcouch.misc.UriUtil;

public class CallFunctionExpression implements Expression {
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

	public Object eval() {
		Object f = funcExpression.eval();
		if( f == null ) throw new RuntimeException("No such function: " + funcExpression.toString() );
		if( !(f instanceof ActiveFunction) ) throw new RuntimeException( "Object returned by " + funcExpression.toString() + " is not an ActiveFunction");
		return ((ActiveFunction)f).call(argumentExpressions);
	}
}
