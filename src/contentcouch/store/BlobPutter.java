package contentcouch.store;

import contentcouch.data.Blob;

public interface BlobPutter {
	public void put( String name, Blob b );
}
