package togos.swf2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import togos.mf.api.Callable;
import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import contentcouch.framework.BaseRequestHandler;

public class NameTranslator extends BaseRequestHandler {
	Callable backingCallable;

	String frontPath;
	String backPath;
	String directoryIndex;
	ArrayList autoAppendPaths = new ArrayList();
	
	public NameTranslator(Callable backingCallable, Map config) {
		this.backingCallable = backingCallable;
		
		this.frontPath = (String)config.get("path");
		this.backPath = (String)config.get("translatedPath");
		this.directoryIndex = (String)config.get("directoryIndex");
		
		Object autoAppendPathse = config.get("autoAppendPaths");
		if( autoAppendPathse instanceof String ) {
			String[] app = ((String)autoAppendPathse).split(",");
			for( int i=0; i<app.length; ++i ) {
				autoAppendPaths.add(app[i]);
			}
		} else if( autoAppendPathse instanceof List ) {
			autoAppendPaths.addAll((List)autoAppendPathse);
		} else {
			autoAppendPaths.add("");
		}
	}
	
	public Response call( Request req ) {
		String rn = req.getResourceName();
		if( !rn.startsWith(frontPath) ) return BaseResponse.RESPONSE_UNHANDLED;

		String mappedPath = backPath + rn.substring(frontPath.length());
		
		if( mappedPath.endsWith("/") ) mappedPath += directoryIndex;
		
		for( Iterator i=autoAppendPaths.iterator(); i.hasNext(); ) {
			String app = (String)i.next();
			Response bres = backingCallable.call( new BaseRequest(req,mappedPath+app) );
			if( bres.getStatus() != ResponseCodes.UNHANDLED && bres.getStatus() != ResponseCodes.DOES_NOT_EXIST ) return bres;
		}
		return BaseResponse.RESPONSE_UNHANDLED;
	}
}
