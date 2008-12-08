package contentcouch.store;

import contentcouch.data.Blob;

public interface BlobStore {
	public void put( String name, Blob blob );
}
