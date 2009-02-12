package contentcouch.data;

import java.util.Map;

public interface Directory {
	public interface Entry {
		public Object getTarget();
		public String getTargetType();
		public String getName();
		public long getSize();
		public long getLastModified();
	}
	
	public Map getEntries();
}
