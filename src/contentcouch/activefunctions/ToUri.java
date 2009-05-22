package contentcouch.activefunctions;

import java.util.Map;

import contentcouch.active.BaseActiveFunction;
import contentcouch.active.Expression;

public class ToUri extends BaseActiveFunction {
	public Object call(Map argumentExpressions) {
		Object o = getArgumentValue(argumentExpressions, "operand", null);
		if( o == null ) return null;
		if( o instanceof Expression ) {
			return ((Expression)o).toUri();
		}
		throw new RuntimeException("Can't to-uri " + o.getClass().getName());
	}
}
