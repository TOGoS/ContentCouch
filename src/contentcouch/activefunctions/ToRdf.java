package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.rdf.RdfUtil;

public class ToRdf extends BaseActiveFunction {
	public Response call( Request req, Map argumentExpressions ) {
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL,
			RdfUtil.toRdfNode(getArgumentValue(req, argumentExpressions, "operand", null), null)
		);
	}
}
