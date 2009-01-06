package contentcouch.app;

import java.io.File;

import contentcouch.data.Blob;
import contentcouch.store.BlobGetter;
import contentcouch.store.BlobPutter;
import contentcouch.store.BlobSink;
import contentcouch.store.FileBlobMap;
import contentcouch.store.Sha1BlobStore;
import contentcouch.xml.RDF;

public class ContentCouchRepository implements BlobGetter, BlobSink {
	protected String path;
	
	// TODO: Allow repo to be accessed remotely (http, freenet, etc) 
	protected BlobGetter dataBlobSource;
	protected BlobSink dataBlobSink;
	protected BlobPutter headBlobPutter;
	protected BlobGetter headBlobSource;
	
	public ContentCouchRepository( String path ) {
		this.path = path;
		Sha1BlobStore bs = new Sha1BlobStore( new FileBlobMap(path + "/data") );
		dataBlobSource = bs;
		dataBlobSink = bs;
		FileBlobMap hs = new FileBlobMap(path + "/heads");
		headBlobPutter = hs;
		headBlobSource = hs;
	}
	
	protected void mkdirs(File dir) {
		if( !dir.exists() ) dir.mkdirs();
	}
	protected void mkdirs(String path) {
		mkdirs(new File(path));
	}

	public void initialize() {
		mkdirs( new File(path).getParentFile() );
	}
	
	public String push( Blob blob ) {
		return dataBlobSink.push(blob);
	}
	
	public Blob get( String blobId ) {
		return dataBlobSource.get(blobId);
	}
	
	public void putHead( String headName, Blob headData ) {
		headBlobPutter.put(headName, headData);
	}
	
	public void putHead( String headName, RDF.RdfNode headInfo ) {
		// TODO!
	}
	
	public static ContentCouchRepository getIfExists( String path ) {
		if( new File(path + "/ccouch-repo").exists() ) {
			return new ContentCouchRepository( path );
		} else {
			throw new RuntimeException( "CCouch repository does not exist at " + path );
		}
	}
	
	public static ContentCouchRepository create( String path ) {
		if( new File(path + "/ccouch-repo").exists() ) {
			throw new RuntimeException( "CCouch repository already exists at " + path );
		} else {
			ContentCouchRepository repo = new ContentCouchRepository( path );
			repo.initialize();
			return repo;
		}
	}
}
