package togos.swf2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import togos.mf.api.Callable;
import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import togos.mf.value.Arguments;
import contentcouch.framework.BaseRequestHandler;

public class SwfFrontRequestHandler extends BaseRequestHandler {
	protected Map components = new HashMap();

	public SwfFrontRequestHandler() {
	}
	
	public void putComponent( String name, Object c ) {
		components.put(name,c);
	}
	
	protected String getUriPreQueryPart( String uri ) {
		String[] parts = uri.split("[\\?\\#]",2);
		return parts[0];
	}
	
	public String getExternalUri( Request req, String dest ) {
		if( dest == null ) return dest;
		if( dest.startsWith(SwfNamespace.SERVLET_PATH_URI_PREFIX) ) {
			String source = req.getResourceName();
			if( !source.startsWith(SwfNamespace.SERVLET_PATH_URI_PREFIX) ) {
				throw new RuntimeException( "Current URI is not a servlet URI! " + source );
			}
			
			String destP = dest.substring(SwfNamespace.SERVLET_PATH_URI_PREFIX.length());
			if( destP.charAt(0) == '/' ) destP = destP.substring(1);
			
			String sourceP = getUriPreQueryPart( source.substring(SwfNamespace.SERVLET_PATH_URI_PREFIX.length()) );
			if( sourceP.charAt(0) == '/' ) sourceP = sourceP.substring(1);
			//String destPath = getUriPreQueryPart( dest );

			String relativePath = "";
			for( int i=1; i<sourceP.length(); ++i ) {
				if( sourceP.charAt(i) == '/' ) relativePath += "../";
			}
			relativePath += destP;
			return relativePath;
		}
		return dest;
	}
	
	public String getExternalComponentUri( Request req, Component component, Arguments args ) {
		return getExternalUri( req, component.getUriFor(args) );
	}

	public String getExternalComponentUri( Request req, String componentName, Arguments args ) {
		Component comp = (Component)components.get(componentName);
		if( comp == null ) throw new RuntimeException("No component called \""+componentName+"\"");
		return getExternalComponentUri( req, comp, args );
	}
	
	public Response call( Request request ) {
		BaseRequest subReq = new BaseRequest(request);
		subReq.putMetadata(SwfNamespace.COMPONENTS, components);
		subReq.putMetadata(SwfNamespace.FRONT, this);
		
		Response res;
		for( Iterator i=components.values().iterator(); i.hasNext(); ) {
			Callable rh = (Callable)i.next();
			res = rh.call(subReq);
			if( res.getStatus() > 0 ) return res;
		}
		return BaseResponse.RESPONSE_UNHANDLED;
	}
}
