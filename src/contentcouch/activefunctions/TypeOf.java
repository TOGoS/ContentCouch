package contentcouch.activefunctions;

import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;

import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.MetadataUtil;

public class TypeOf extends BaseActiveFunction {
	public Response call( Map argumentExpressions ) {
		Response subRes = getArgumentResponse(argumentExpressions, "operand");
		return new BaseResponse(MetadataUtil.getContentType(subRes));
	}
}
