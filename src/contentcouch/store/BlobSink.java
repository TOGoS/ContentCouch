package contentcouch.store;

import contentcouch.data.Blob;

public interface BlobSink {
	public String push( Blob b );
}
