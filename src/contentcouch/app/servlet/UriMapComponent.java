package contentcouch.app.servlet;

import java.util.ArrayList;
import java.util.Iterator;

import togos.rra.Arguments;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;

public class UriMapComponent extends BaseSwfComponent {
	String externalUriPrefix;
	String internalUriPrefix;
	boolean ignoreMisses;
	String directoryIndex = "index";
	ArrayList autoPostfixes = new ArrayList();
	
	public UriMapComponent( String externalUriPrefix, String internalUriPrefix, boolean ignoreMisses, String directoryIndex ) {
		this.externalUriPrefix = externalUriPrefix;
		this.internalUriPrefix = internalUriPrefix;
		this.ignoreMisses = ignoreMisses;
		this.directoryIndex = directoryIndex;
	}
	
	public void addAutoPostfix( String pf ) {
		autoPostfixes.add(pf);
	}
	
	////
	
	public String getUriFor(Arguments args) {
		return null;
	}

	public Response handleRequest(Request request) {
		String uri = request.getUri();
		if( uri.startsWith(externalUriPrefix) ) {
			String internalUri = internalUriPrefix + uri.substring(externalUriPrefix.length());
			if( internalUri.endsWith("/") ) internalUri += directoryIndex;
			for( Iterator i=autoPostfixes.iterator(); i.hasNext(); ) {
				Response res = forwardRequest( request, internalUri + (String)i.next() );
				if( res.getStatus() == Response.STATUS_DOESNOTEXIST && ignoreMisses ) continue;
				if( res.getStatus() != Response.STATUS_UNHANDLED ) return res;
			}
		}
		return BaseResponse.RESPONSE_UNHANDLED;
	}
}
