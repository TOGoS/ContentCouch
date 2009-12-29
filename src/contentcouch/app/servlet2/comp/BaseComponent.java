package contentcouch.app.servlet2.comp;

import java.util.Iterator;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import togos.mf.value.Arguments;
import togos.swf2.Component;
import togos.swf2.SwfNamespace;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;

public abstract class BaseComponent implements Component {
	protected Map properties;
	protected String handlePath;
	
	protected boolean shouldHandle( Request req ) {
		if( handlePath == null ) return false;
		String rn = req.getResourceName();
		return rn.startsWith(this.handlePath);
	}
	
	protected abstract Response _call( Request req ); 
	
	public Response call( Request req ) {
		if( !shouldHandle(req) ) return BaseResponse.RESPONSE_UNHANDLED;
		BaseRequest subReq = new BaseRequest(req);
		subReq.putContextVar(SwfNamespace.COMPONENT, this);
		return _call( subReq );
	}
	
	public Map getProperties() {
		return properties;
	}
	
	protected String encodeArguments( Arguments args ) {
		String encoded = "";
		for( Iterator i=args.getNamedArguments().entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			if( encoded.length() > 0 ) encoded += "&";
			encoded += UriUtil.uriEncode(ValueUtil.getString(e.getKey())) + "=" + UriUtil.uriEncode(ValueUtil.getString(e.getValue()));
		}
		return encoded;
	}
	
	protected String encodePathAndArguments( String path, Arguments args ) {
		String argStr = encodeArguments(args);
		return (argStr.length() > 0) ? path + "?" + argStr : path; 
	}
	
	public String getUriFor( Arguments args ) {
		return encodePathAndArguments(this.handlePath, args);
	}
}
