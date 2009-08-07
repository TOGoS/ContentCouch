package togos.mf.base;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import togos.mf.Request;

public class BaseRequest implements Request {
	public String verb;
	public String uri;
	public Object content;
	
	public Map contentMetadata = Collections.EMPTY_MAP;
	protected boolean contentMetadataClean = true;
	
	public Map metadata = Collections.EMPTY_MAP;
	protected boolean metadataClean = true;
	
	public Map contextVars = Collections.EMPTY_MAP;
	protected boolean contextVarsClean = true;
	
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
		this.contentMetadataClean = true;
	}

	/** Make one just like req */
	public BaseRequest( Request req ) {
		this.verb = req.getVerb();
		this.uri = req.getUri();
		this.content = req.getContent();
		this.contentMetadata = req.getContentMetadata();
		this.contentMetadataClean = true;
		this.metadata = req.getMetadata();
		this.metadataClean = true;
		this.contextVars = req.getContextVars();
		this.contextVarsClean = true;
	}

	/** Make one just like req, but with a different URI */
	public BaseRequest( Request req, String uri ) {
		this.verb = req.getVerb();
		this.uri = uri;
		this.content = req.getContent();
		this.contentMetadata = req.getContentMetadata();
		this.contentMetadataClean = true;
		this.metadata = req.getMetadata();
		this.metadataClean = true;
		this.contextVars = req.getContextVars();
		this.contextVarsClean = true;
	}
	
	public String getVerb() {  return verb;  }
	public String getUri() {  return uri;  }
	public Object getContent() {  return content;  }
	public Map getContentMetadata() {  return contentMetadata;  }
	public Map getMetadata() {  return metadata;  }
	public Map getContextVars() {  return contextVars;  }
	
	public void putContentMetadata(String key, Object value) {
		if( contentMetadataClean ) {
			contentMetadata = new HashMap(contentMetadata);
			contentMetadataClean = false;
		}
		contentMetadata.put(key, value);
	}
	
	public void clearContentMetadata() {
		contentMetadata = Collections.EMPTY_MAP;
		contentMetadataClean = true;
	}
	
	public void putMetadata(String key, Object value) {
		if( metadataClean ) {
			metadata = new HashMap(metadata);
			metadataClean = false;
		}
		metadata.put(key, value);
	}

	public void putContextVar(String key, Object value) {
		if( contextVarsClean ) {
			contextVars = new HashMap(contextVars);
			contextVarsClean = false;
		}
		contextVars.put(key, value);
	}

}
