package contentcouch.app.servlet;

import java.util.HashMap;
import java.util.Map;

import contentcouch.store.TheGetter;

import togos.rra.BaseRequest;
import togos.rra.Request;
import togos.rra.Response;
import togos.swf2.Component;

public abstract class BaseSwfComponent implements Component {
	HashMap metadata = new HashMap();
	public void putMetadata( String k, Object v  ) {
		metadata.put( k, v );
	}
	
	public Map getMetadata() {  return metadata;  }
	
	protected Response forwardRequest( Request request, String newUri ) {
		BaseRequest subReq = new BaseRequest(request);
		subReq.uri = newUri;
		return TheGetter.handleRequest(subReq);
	}
}
