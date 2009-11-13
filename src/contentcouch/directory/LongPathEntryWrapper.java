package contentcouch.directory;

import contentcouch.value.Directory;

public class LongPathEntryWrapper implements Directory.Entry, HasLongPath {
	Directory.Entry backingEntry;
	String longPath;
	
	public LongPathEntryWrapper( Directory.Entry e, String longPath ) {
		this.backingEntry = e;
		this.longPath = longPath;
	}
	
	public String getName() { return backingEntry.getName(); }
	public Object getTarget() { return backingEntry.getTarget(); }
	public long getTargetLastModified() { return backingEntry.getTargetLastModified(); }
	public long getTargetSize() { return backingEntry.getTargetSize(); }
	public String getTargetType() { return backingEntry.getTargetType(); }
	
	public String getLongPath() { return longPath; }
}
