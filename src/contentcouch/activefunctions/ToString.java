package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;

public class ToString extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		Object o = getArgumentValue(argumentExpressions, "operand", null);
		if( o == null ) return null;
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, o.toString(), "text/plain");
	}
}
