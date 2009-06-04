package contentcouch.activefunctions;

import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.rdf.RdfUtil;

public class ToRdf extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		return new BaseResponse(Response.STATUS_NORMAL,
			RdfUtil.toRdfNode(getArgumentValue(argumentExpressions, "operand", null), null)
		);
	}
}
