package contentcouch.active;

import java.util.HashMap;
import java.util.Map;

import contentcouch.active.expression.Expression;

import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.RequestHandler;
import togos.rra.Response;

public class ActiveUriResolver implements RequestHandler {
	public Map namedActiveFunctions = new HashMap();
	
	public ActiveUriResolver() {
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
