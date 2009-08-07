package contentcouch.misc;

import contentcouch.framework.BaseRequestHandler;
import togos.mf.RequestVerbs;
import togos.mf.ResponseCodes;
import togos.mf.Request;
import togos.mf.Response;
import togos.mf.base.BaseResponse;

public class ContextVarRequestHandler extends BaseRequestHandler {
	public Response call(Request req) {
		if( !req.getUri().startsWith("x-context-var:") ) return BaseResponse.RESPONSE_UNHANDLED;
		
		String varName = UriUtil.uriDecode(req.getUri().substring("x-context-var:".length()));
		
		if( RequestVerbs.VERB_GET.equals(req.getVerb()) ) {
			return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, req.getContextVars().get(varName));
		} else if( RequestVerbs.VERB_PUT.equals(req.getVerb()) ) {
			req.getContextVars().put(varName, req.getContent());
			return new BaseResponse();
		} else {
			return BaseResponse.RESPONSE_UNHANDLED;
		}
	}
}
