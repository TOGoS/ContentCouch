package contentcouch.activefunctions;

import java.util.Map;

import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;;

public class Explore extends BaseActiveFunction {
	
	public Response call(Map argumentExpressions) {
		// TODO: Do something!
		return null;
	}

	//// Path simplification ////
	
	protected String getPathArgumentName() {
		return "operand";
	}
}
