package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;

import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.MetadataUtil;

public class TypeOf extends BaseActiveFunction {
	public Response call( Request req, Map argumentExpressions ) {
		Response subRes = getArgumentResponse(req, argumentExpressions, "operand");
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, MetadataUtil.getContentType(subRes));
	}
}
