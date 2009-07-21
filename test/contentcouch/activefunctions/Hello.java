package contentcouch.activefunctions;

import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;

import contentcouch.active.ActiveFunction;

public class Hello implements ActiveFunction {
	public Response call( Map argumentExpressions ) {
		return new BaseResponse(Response.STATUS_NORMAL, "Hello, world", "text/plain");
	}
}
