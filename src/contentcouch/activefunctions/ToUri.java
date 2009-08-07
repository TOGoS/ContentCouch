package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.ResponseCodes;
import togos.mf.Response;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.Expression;

public class ToUri extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		Object o = getArgumentValue(argumentExpressions, "operand", null);
		if( o == null ) return null;
		if( o instanceof Expression ) {
			return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, ((Expression)o).toUri(), "text/plain");
		}
		throw new RuntimeException("Can't to-uri " + o.getClass().getName());
	}
}
