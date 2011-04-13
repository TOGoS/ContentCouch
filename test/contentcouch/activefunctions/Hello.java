package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;

import contentcouch.active.ActiveFunction;

public class Hello implements ActiveFunction {
	public Response call( Request req, Map argumentExpressions ) {
		return new BaseResponse(ResponseCodes.NORMAL, "Hello, world", "text/plain");
	}
	public boolean isConstant( Map argumentExpressions ) {
		return true;
	}
}
