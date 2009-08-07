package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.ResponseCodes;
import togos.mf.Response;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.ValueUtil;

public class Delay extends BaseActiveFunction {

	public Response call(Map argumentExpressions) {
		Number delay = ValueUtil.getNumber(getArgumentValue(argumentExpressions, "operand", new Integer(5)));
		long millis = (long)(delay.doubleValue() * 1000);
		try {
			Thread.sleep(millis);
		} catch( InterruptedException e ) {
			throw new RuntimeException(e);
		}
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, "Delayed " + millis + " milliseconds", "text/plain");
	}

}
