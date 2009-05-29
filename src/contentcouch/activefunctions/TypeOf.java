package contentcouch.activefunctions;

import java.util.Map;

import contentcouch.active.BaseActiveFunction;

public class TypeOf extends BaseActiveFunction {
	public Object call( Map argumentExpressions ) {
		Object obj = getArgumentValue(argumentExpressions, "operand", null);
		
		if( obj instanceof String ) {
			return "text/plain";
		}
		// TODO: Try to guess type
		return null;
	}
}
