package contentcouch.activefunctions;

import java.util.Map;

import contentcouch.active.BaseActiveFunction;

public class Quote extends BaseActiveFunction {
	public Object call(Map argumentExpressions) {
		return argumentExpressions.get("operand");
	}
}
