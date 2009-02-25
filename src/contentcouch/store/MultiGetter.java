package contentcouch.store;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import contentcouch.value.Blob;

public class MultiGetter implements Getter, StoreFileGetter {
	protected List subGetters;
	
	public MultiGetter() {
		this.subGetters = new ArrayList();
	}
	
	public void addGetter(Getter bg) {
		if( bg != null ) this.subGetters.add(0, bg);
	}
	
	public Object get(String identifier) {
		for( Iterator i=subGetters.iterator(); i.hasNext(); ) {
			Object obj = ((Getter)i.next()).get(identifier);
			if( obj != null ) return obj;
		}
		return null;
	}

	public File getStoreFile(String identifier) {
		for( Iterator i=subGetters.iterator(); i.hasNext(); ) {
			Object g = i.next();
			if( g instanceof StoreFileGetter ) {
				File f = ((StoreFileGetter)g).getStoreFile(identifier);
				if( f != null ) return f;
			}
		}
		return null;
	}

	public File getStoreFile(Blob blob) {
		for( Iterator i=subGetters.iterator(); i.hasNext(); ) {
			Object g = i.next();
			if( g instanceof StoreFileGetter ) {
				File f = ((StoreFileGetter)g).getStoreFile(blob);
				if( f != null ) return f;
			}
		}
		return null;
	}
}
