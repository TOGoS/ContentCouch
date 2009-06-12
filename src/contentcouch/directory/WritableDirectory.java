package contentcouch.directory;

import contentcouch.value.Directory;

public interface WritableDirectory extends Directory {
	public void addDirectoryEntry( Directory.Entry newEntry );
}
