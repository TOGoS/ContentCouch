package contentcouch.activefunctions;

import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;

public class ToString extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		Object o = getArgumentValue(argumentExpressions, "operand", null);
		if( o == null ) return null;
		return new BaseResponse(Response.STATUS_NORMAL, o.toString(), "text/plain");
	}
}
