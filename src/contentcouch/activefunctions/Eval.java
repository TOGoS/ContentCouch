package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.Context;
import contentcouch.misc.ValueUtil;
import contentcouch.store.TheGetter;

public class Eval extends BaseActiveFunction {
	public Response call( Map argumentExpressions ) {
		Object resolveThis = getArgumentValue(argumentExpressions, "operand", null);
		if( resolveThis == null ) return BaseResponse.RESPONSE_UNHANDLED;
		String uri = ValueUtil.getString(resolveThis);
		BaseRequest req = new BaseRequest(RequestVerbs.VERB_GET, uri);
		req.contextVars = Context.getInstance();
		return TheGetter.handleRequest(req);
	}
}
