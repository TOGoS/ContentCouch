package contentcouch.activefunctions;

import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.Expression;

public class ToUri extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		Object o = getArgumentValue(argumentExpressions, "operand", null);
		if( o == null ) return null;
		if( o instanceof Expression ) {
			return new BaseResponse(Response.STATUS_NORMAL, ((Expression)o).toUri(), "text/plain");
		}
		throw new RuntimeException("Can't to-uri " + o.getClass().getName());
	}
}
