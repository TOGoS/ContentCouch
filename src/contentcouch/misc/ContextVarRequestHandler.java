package contentcouch.misc;

import togos.rra.BaseRequestHandler;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;

public class ContextVarRequestHandler extends BaseRequestHandler {
	public Response handleRequest(Request req) {
		if( !req.getUri().startsWith("x-context-var:") ) return BaseResponse.RESPONSE_UNHANDLED;
		
		String varName = UriUtil.uriDecode(req.getUri().substring("x-context-var:".length()));
		
		if( Request.VERB_GET.equals(req.getVerb()) ) {
			return new BaseResponse(req.getContextVars().get(varName));
		} else if( Request.VERB_PUT.equals(req.getVerb()) ) {
			req.getContextVars().put(varName, req.getContent());
			return new BaseResponse();
		} else {
			return BaseResponse.RESPONSE_UNHANDLED;
		}
	}
}
