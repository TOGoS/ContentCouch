package contentcouch.store;

import contentcouch.data.Blob;

public class NoopBlobPutter implements BlobPutter {
	public void put(String name, Blob b) {
	}
}
