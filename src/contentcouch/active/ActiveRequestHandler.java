package contentcouch.active;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.base.BaseResponse;
import contentcouch.active.expression.Expression;
import contentcouch.framework.BaseRequestHandler;

public class ActiveRequestHandler extends BaseRequestHandler {
	public ActiveRequestHandler() {
	}
	
	public Response call( Request req ) {
		String uri = req.getResourceName();
		if( uri.startsWith(ActiveUtil.ACTIVE_URI_PREFIX) || uri.startsWith("(") || uri.startsWith("\"") ) {
			Expression e = ActiveUtil.parseExpression( uri );
			return e.eval(req);
		}
		return BaseResponse.RESPONSE_UNHANDLED;
	}
}
