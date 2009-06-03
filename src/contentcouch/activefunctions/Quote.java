package contentcouch.activefunctions;

import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;

import contentcouch.active.BaseActiveFunction;

public class Quote extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		return new BaseResponse(argumentExpressions.get("operand"));
	}
}
