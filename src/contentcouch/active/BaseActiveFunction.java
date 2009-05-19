package contentcouch.active;

import java.util.Map;


public abstract class BaseActiveFunction implements ActiveFunction {
	protected Object getArgumentValue( Map context, Map argumentExpressions, String name, Object defaultValue ) {
		Expression e = (Expression)argumentExpressions.get(name);
		if( e == null ) return defaultValue;
		return e.eval(context);
	}
}
