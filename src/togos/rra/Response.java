package togos.rra;

import java.util.Map;

public interface Response extends ContentAndMetadata {
	public static final int STATUS_UNHANDLED = 0;
	public static final int STATUS_NORMAL = 200;
	public static final int STATUS_DOESNOTEXIST = 404;
	
	public int getStatus();
	public Object getContent();
	public Map getContentMetadata();
	public Map getMetadata();
}
