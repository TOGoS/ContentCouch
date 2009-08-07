package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.ResponseCodes;
import togos.mf.Response;
import togos.mf.base.BaseResponse;

import contentcouch.active.BaseActiveFunction;

public class Quote extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, argumentExpressions.get("operand"));
	}
}
