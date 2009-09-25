package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.ValueUtil;

public class Delay extends BaseActiveFunction {

	public Response call(Request req, Map argumentExpressions) {
		Number delay = ValueUtil.getNumber(getArgumentValue(req, argumentExpressions, "operand", new Integer(5)));
		long millis = (long)(delay.doubleValue() * 1000);
		try {
			Thread.sleep(millis);
		} catch( InterruptedException e ) {
			throw new RuntimeException(e);
		}
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, "Delayed " + millis + " milliseconds", "text/plain");
	}

}
