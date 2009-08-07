package togos.mf;

import java.util.Map;

/** Common interface for objects that contain both content and metadata
 * about the content */
public interface ContentAndMetadata {
	public Object getContent();
	public Map getContentMetadata();
}
