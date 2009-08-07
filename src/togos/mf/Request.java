package togos.mf;

import java.util.Map;

public interface Request extends ContentAndMetadata {
	public String getVerb();
	public String getUri();
	public Object getContent();
	public Map getContentMetadata();
	public Map getMetadata();
	public Map getContextVars();
}
