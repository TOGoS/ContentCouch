package contentcouch.store;

import contentcouch.data.Blob;

public interface BlobGetter {
	public Blob get( String identifier );
}
