package contentcouch.value;

import java.util.Set;

public interface Directory {
	public interface Entry {
		/** Returns the target object or a Ref to it */
		public Object getTarget();
		/** Returns the name of the object type ('Blob' or 'Directory') of the target */
		public String getTargetType();
		/** Returns the filename */
		public String getName();
		/** Returns the size (in bytes) of the target blob.
		 * If unknown of not applicable (if target is not a blob), should return -1. */
		public long getTargetSize();
		/** Returns the timestamp at which the target was last created or modified.
		 * If unknown of not applicable (if target is a directory), should return -1. */
		public long getLastModified();
	}
	
	//public Map getEntries();
	public Set getDirectoryEntrySet();
	public Entry getDirectoryEntry(String name);
}
