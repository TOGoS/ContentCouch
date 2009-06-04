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
	
	/** Make a blank one */
	public BaseRequest() { }

	/** Make one with the given verb and URI */
	public BaseRequest( String verb, String uri ) {
		this.verb = verb;
		this.uri = uri;
	}

	/** Make one with the given verb, URI, content, and content metadata */
	public BaseRequest( String verb, String uri, Object content, Map contentMetadata ) {
		this.verb = verb;
		this.uri = uri;
		this.content = content;
		this.contentMetadata = contentMetadata;
	}

	/** Make one just like req */
	public BaseRequest( Request req ) {
		this.verb = req.getVerb();
		this.uri = req.getUri();
		this.content = req.getContent();
		this.contentMetadata = req.getContentMetadata();
		this.metadata = req.getMetadata();
	}

	/** Make one just like req, but with a different URI */
	public BaseRequest( Request req, String uri ) {
		this.verb = req.getVerb();
		this.uri = uri;
		this.content = req.getContent();
		this.contentMetadata = req.getContentMetadata();
		this.metadata = req.getMetadata();
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
