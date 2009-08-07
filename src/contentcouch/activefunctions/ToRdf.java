package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.ResponseCodes;
import togos.mf.Response;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.rdf.RdfUtil;

public class ToRdf extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL,
			RdfUtil.toRdfNode(getArgumentValue(argumentExpressions, "operand", null), null)
		);
	}
}
