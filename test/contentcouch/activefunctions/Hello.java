package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;

import contentcouch.active.ActiveFunction;

public class Hello implements ActiveFunction {
	public Response call( Map argumentExpressions ) {
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, "Hello, world", "text/plain");
	}
}
