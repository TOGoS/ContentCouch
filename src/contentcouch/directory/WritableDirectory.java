package contentcouch.directory;

import java.util.Map;

import contentcouch.value.Directory;

public interface WritableDirectory extends Directory {
	public void addDirectoryEntry( Directory.Entry newEntry, Map requestMetadata );
}
