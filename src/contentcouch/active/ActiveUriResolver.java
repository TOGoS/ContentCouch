package contentcouch.active;

import java.util.HashMap;
import java.util.Map;

import contentcouch.active.expression.Expression;

import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.RequestHandler;
import togos.rra.Response;

public class ActiveUriResolver implements RequestHandler {
	public static final char[] HEXCHARS = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	public Map namedActiveFunctions = new HashMap();
	
	public ActiveUriResolver() {
	}
	
	public Response handleRequest( Request req ) {
		String uri = req.getUri();
		if( uri.startsWith(ActiveUtil.ACTIVE_URI_PREFIX) || uri.startsWith("(") || uri.startsWith("\"") ) {
			Expression e = ActiveUtil.parseExpression( uri );
			//Map context = Context.getInstance();
			//context.put(Context.GENERIC_GETTER_VAR, getter);
			//context.put(GetFunctionByNameExpression.FUNCTION_MAP_VARNAME, this.namedActiveFunctions);
			return e.eval();
		}
		return BaseResponse.RESPONSE_UNHANDLED;
	}
}
