package contentcouch.active;

import java.util.Iterator;
import java.util.Map;

public class CallFunctionExpression implements Expression {
	Expression funcExpression;
	Map argumentExpressions;
	
	public CallFunctionExpression( Expression funcExpression, Map argumentExpressions ) {
		this.funcExpression = funcExpression;
		this.argumentExpressions = argumentExpressions;
	}
	
	public String toString() {
		String res = "(";
		if( funcExpression instanceof GetFunctionByNameExpression ) {
			res += ((GetFunctionByNameExpression)funcExpression).funcName;
		} else {
			res += funcExpression.toString();
		}
		for( Iterator i=argumentExpressions.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			res += " " + e.getKey().toString() + "=" + e.getValue().toString(); 
		}
		res += ")";
		return res;
	}
	
	public Object eval( Map context ) {
		Object f = funcExpression.eval(context);
		if( f == null ) throw new RuntimeException("No such function: " + funcExpression.toString() );
		if( !(f instanceof ActiveFunction) ) throw new RuntimeException( "Object returned by " + funcExpression.toString() + " is not an ActiveFunction");
		return ((ActiveFunction)f).call(context, argumentExpressions);
	}
}
