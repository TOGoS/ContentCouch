package contentcouch.store;

import java.io.File;

public interface StoreFileGetter {
	/** Returns the file (which may not actually exist) where something with the given
	 * identifier would be stored. */
	public File getStoreFile( String identifier );
}
