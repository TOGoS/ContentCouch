package togos.rra;

import java.util.Map;

public interface Request {
	public String getVerb();
	public String getUri();
	public Object getContent();
	public Map getContentMetadata();
	public Map getMetadata();
}
