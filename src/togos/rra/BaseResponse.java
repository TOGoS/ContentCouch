package togos.rra;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import contentcouch.misc.ValueUtil;
import contentcouch.rdf.DcNamespace;

public class BaseResponse implements Response {
	public static final BaseResponse RESPONSE_UNHANDLED = new BaseResponse(Response.STATUS_UNHANDLED, "Not handled", "text/plain");
	
	public int status;
	public Object content;
	public Map contentMetadata = Collections.EMPTY_MAP;
	public Map metadata = Collections.EMPTY_MAP;
	
	public BaseResponse() {
		this(Response.STATUS_NORMAL, null);
	}
	
	public BaseResponse( Object content ) {
		this.status = Response.STATUS_NORMAL;
		this.content = content;
	}
	
	public BaseResponse( int status, Object content ) {
		this.status = status;
		this.content = content;
	}

	public BaseResponse( int status, Object content, Response inheritFrom ) {
		this(status, content);
		if( ValueUtil.getBoolean(inheritFrom.getMetadata().get(RraNamespace.CACHEABLE),false) ) {
			putMetadata(RraNamespace.CACHEABLE, Boolean.TRUE);
		}
	}
	
	public BaseResponse( int status, Object content, String contentType ) {
		this(status,content);
		putContentMetadata(DcNamespace.DC_FORMAT, contentType);
	}

	public int getStatus() {  return status;  }
	public Object getContent() {  return content;  }
	public Map getContentMetadata() {  return contentMetadata;  }
	public Map getMetadata() {  return metadata;  }
	
	public void putMetadata(String key, Object value) {
		if( metadata == Collections.EMPTY_MAP ) metadata = new HashMap();
		metadata.put(key, value);
	}

	public void putContentMetadata(String key, Object value) {
		if( contentMetadata == Collections.EMPTY_MAP ) contentMetadata = new HashMap();
		contentMetadata.put(key, value);
	}
}
