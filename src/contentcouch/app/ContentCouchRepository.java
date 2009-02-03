package contentcouch.app;

import java.io.File;

import contentcouch.data.Blob;
import contentcouch.file.FileUtil;
import contentcouch.store.Getter;
import contentcouch.store.Putter;
import contentcouch.store.Pusher;
import contentcouch.store.FileBlobMap;
import contentcouch.store.Sha1BlobStore;
import contentcouch.xml.RDF;

public class ContentCouchRepository implements Getter, Pusher {
	protected String path;
	
	// TODO: Allow repo to be accessed remotely (http, freenet, etc) 
	protected Getter dataBlobSource;
	protected Pusher dataBlobSink;
	protected Putter headBlobPutter;
	protected Getter headBlobSource;
	
	public ContentCouchRepository( String path ) {
		this.path = path;
		Sha1BlobStore bs = new Sha1BlobStore( new FileBlobMap(path + "/data") );
		dataBlobSource = bs;
		dataBlobSink = bs;
		FileBlobMap hs = new FileBlobMap(path + "/heads");
		headBlobPutter = hs;
		headBlobSource = hs;
	}
	
	public void initialize() {
		FileUtil.mkParentDirs( new File(path) );
	}
	
	public String push( Object obj ) {
		return dataBlobSink.push(obj);
	}
	
	public Object get( String blobId ) {
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
