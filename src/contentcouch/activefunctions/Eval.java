package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.framework.TheGetter;
import contentcouch.misc.ValueUtil;

public class Eval extends BaseActiveFunction {
	public Response call( Request req, Map argumentExpressions ) {
		Object resolveThis = getArgumentValue(req, argumentExpressions, "operand", null);
		if( resolveThis == null ) return BaseResponse.RESPONSE_UNHANDLED;
		String uri = ValueUtil.getString(resolveThis);
		BaseRequest subReq = new BaseRequest(req, uri);
		return TheGetter.call(subReq);
	}
}
