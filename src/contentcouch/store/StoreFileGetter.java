package contentcouch.store;

import java.io.File;

import contentcouch.value.Blob;

public interface StoreFileGetter {
	/** Returns the file (which may not actually exist) where something with the given
	 * identifier would be stored. */
	public File getStoreFile( String identifier );

	/** Returns the file (which may not actually exist) where the given blob would be stored. */
	public File getStoreFile( Blob blob );
}
