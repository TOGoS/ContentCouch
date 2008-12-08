package contentcouch.app;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import contentcouch.data.Blob;
import contentcouch.store.BlobSink;
import contentcouch.store.BlobSource;
import contentcouch.store.Sha1BlobStore;

public class Datastore implements BlobSource, BlobSink {
	String datastoreDir;
	String defaultChannel = "data";
	Map stores;
	
	public Datastore( String dir ) {
		this.datastoreDir = dir;
		this.stores = new HashMap();
		
		this.stores.put("data", new Sha1BlobStore(dir + "/data/") );
		this.stores.put("metadata", new Sha1BlobStore(dir + "/metadata/") );
		this.stores.put("metametadata", new Sha1BlobStore(dir + "/metametadata/") );
	}
	
	public String push( String channel, Blob blob ) {
		BlobSink store = (BlobSink)stores.get(channel);
		if( store == null ) {
			throw new RuntimeException("No such blob store: " + channel);
		}
		return store.push( blob );
	}
	
	public String push( Blob blob ) {
		return push( defaultChannel, blob );
	}
	
	public Blob get( String uri ) {
		for( Iterator storeit=stores.values().iterator(); storeit.hasNext(); ) {
			Object so = storeit.next();
			if( so instanceof BlobSource ) {
				Blob b = ((BlobSource)so).get( uri );
				if( b != null ) return b;
			}
		}
		return null;
	}
}
