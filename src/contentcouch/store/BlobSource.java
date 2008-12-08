package contentcouch.store;

import contentcouch.data.Blob;

public interface BlobSource {
	public Blob get( String identifier );
}
