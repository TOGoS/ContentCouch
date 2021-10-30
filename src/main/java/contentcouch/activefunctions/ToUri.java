package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.Expression;

public class ToUri extends BaseActiveFunction {
	public Response call(Request req, Map argumentExpressions) {
		Object o = getArgumentValue(req, argumentExpressions, "operand", null);
		if( o == null ) return null;
		if( o instanceof Expression ) {
			return new BaseResponse(ResponseCodes.NORMAL, ((Expression)o).toUri(), "text/plain");
		}
		throw new RuntimeException("Can't to-uri " + o.getClass().getName());
	}
}
