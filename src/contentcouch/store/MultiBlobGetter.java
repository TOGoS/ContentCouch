package contentcouch.store;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import contentcouch.data.Blob;

public class MultiBlobGetter implements BlobGetter {
	protected List blobGetters;
	
	public MultiBlobGetter() {
		this.blobGetters = new ArrayList();
	}
	
	public void addBlobGetter(BlobGetter bg) {
		if( bg != null ) this.blobGetters.add(0, bg);
	}
	
	public Blob get(String identifier) {
		for( Iterator i=blobGetters.iterator(); i.hasNext(); ) {
			Blob blob = ((BlobGetter)i.next()).get(identifier);
			if( blob != null ) return blob;
		}
		return null;
	}
}
