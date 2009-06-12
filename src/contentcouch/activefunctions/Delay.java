package contentcouch.activefunctions;

import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.ValueUtil;

public class Delay extends BaseActiveFunction {

	public Response call(Map argumentExpressions) {
		Number delay = ValueUtil.getNumber(getArgumentValue(argumentExpressions, "operand", Integer.valueOf(5)));
		long millis = (long)(delay.doubleValue() * 1000);
		try {
			Thread.sleep(millis);
		} catch( InterruptedException e ) {
			throw new RuntimeException(e);
		}
		return new BaseResponse(Response.STATUS_NORMAL, "Delayed " + millis + " milliseconds", "text/plain");
	}

}
