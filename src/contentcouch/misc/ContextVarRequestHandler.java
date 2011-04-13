package contentcouch.misc;

import contentcouch.framework.BaseRequestHandler;
import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;

public class ContextVarRequestHandler extends BaseRequestHandler {
	public Response call(Request req) {
		if( !req.getResourceName().startsWith("x-context-var:") ) return BaseResponse.RESPONSE_UNHANDLED;
		
		String varName = UriUtil.uriDecode(req.getResourceName().substring("x-context-var:".length()));
		
		if( RequestVerbs.GET.equals(req.getVerb()) ) {
			return new BaseResponse(ResponseCodes.NORMAL, req.getMetadata().get(varName));
			/*
		} else if( RequestVerbs.PUT.equals(req.getVerb()) ) {
			req.getMetadata().put(varName, req.getContent());
			return new BaseResponse();
			*/
		} else {
			return BaseResponse.RESPONSE_UNHANDLED;
		}
	}
}
