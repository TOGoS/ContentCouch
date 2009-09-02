package togos.swf2;

import togos.mf.api.CallHandler;
import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import contentcouch.framework.BaseRequestHandler;

public class NameTranslator extends BaseRequestHandler {
	String frontPath;
	String backPath;
	String directoryIndex;
	
	CallHandler backingCallHandler;
	
	public NameTranslator(String frontPath, String backPath, CallHandler backingCallHandler, String directoryIndex) {
		this.frontPath = frontPath;
		this.backPath = backPath;
		this.backingCallHandler = backingCallHandler;
		this.directoryIndex = directoryIndex;
	}
	
	public Response call( Request req ) {
		String rn = req.getResourceName();
		if( !rn.startsWith(frontPath) ) return BaseResponse.RESPONSE_UNHANDLED;

		String mappedPath = backPath + rn.substring(frontPath.length());
		
		if( mappedPath.endsWith("/") ) mappedPath += directoryIndex;
		
		Response bres = backingCallHandler.call( new BaseRequest(req,mappedPath) );
		if( bres.getStatus() == ResponseCodes.RESPONSE_DOESNOTEXIST ) return BaseResponse.RESPONSE_UNHANDLED;
		return bres;
	}
}
