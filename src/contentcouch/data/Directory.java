package contentcouch.data;

import java.util.Map;

public interface Directory {
	public interface Entry {
		public String getName();
		public String getTargetType();
		public long getLastModified();
		public Object getTarget();
	}
	
	public Map getEntries();
}
