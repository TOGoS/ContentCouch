package togos.rra;

import java.util.Map;

public interface Request {
	public static final String VERB_GET = "GET";
	public static final String VERB_HEAD = "HEAD";
	public static final String VERB_PUT = "PUT";
	public static final String VERB_POST = "POST";
	
	public String getVerb();
	public String getUri();
	public Object getContent();
	public Map getContentMetadata();
	public Map getMetadata();
}
