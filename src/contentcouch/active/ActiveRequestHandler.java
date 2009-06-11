package contentcouch.active;

import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.RequestHandler;
import togos.rra.Response;
import contentcouch.active.expression.Expression;

public class ActiveRequestHandler implements RequestHandler {
	public ActiveRequestHandler() {
	}
	
	public Response handleRequest( Request req ) {
		String uri = req.getUri();
		if( uri.startsWith(ActiveUtil.ACTIVE_URI_PREFIX) || uri.startsWith("(") || uri.startsWith("\"") ) {
			Expression e = ActiveUtil.parseExpression( uri );
			Context.pushInstance(req.getContextVars());
			try {
				return e.eval();
			} finally {
				Context.popInstance();
			}
		}
		return BaseResponse.RESPONSE_UNHANDLED;
	}
}
