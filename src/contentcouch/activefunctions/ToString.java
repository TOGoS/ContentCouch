package contentcouch.activefunctions;

import java.util.Map;

import contentcouch.active.BaseActiveFunction;

public class ToString extends BaseActiveFunction {
	public Object call(Map argumentExpressions) {
		Object o = getArgumentValue(argumentExpressions, "operand", null);
		if( o == null ) return null;
		return o.toString();
	}
}
