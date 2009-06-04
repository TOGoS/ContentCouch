package togos.rra;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BaseRequest implements Request {
	public String verb;
	public String uri;
	public Object content;
	public Map contentMetadata = Collections.EMPTY_MAP;
	public Map metadata = Collections.EMPTY_MAP;
	
	public BaseRequest() { }
	
	public BaseRequest( Request req, String uri ) {
		this.verb = req.getVerb();
		this.uri = uri;
		this.content = req.getContent();
		this.contentMetadata = req.getContentMetadata();
		this.metadata = req.getMetadata();
	}

	public BaseRequest( Request req, String uri, Object content ) {
		this.verb = req.getVerb();
		this.uri = uri;
		this.content = content;
		this.metadata = req.getMetadata();
	}

	public BaseRequest( Request req ) {
		this( req, req.getUri() );
	}

	public BaseRequest( String verb, String uri ) {
		this.verb = verb;
		this.uri = uri;
	}
	
	public String getVerb() {  return verb;  }
	public String getUri() {  return uri;  }
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
