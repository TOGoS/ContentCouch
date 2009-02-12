package contentcouch.app;

import java.io.File;

import contentcouch.file.FileUtil;
import contentcouch.http.HttpBlobGetter;
import contentcouch.rdf.RdfNode;
import contentcouch.store.FileBlobMap;
import contentcouch.store.Getter;
import contentcouch.store.MultiGetter;
import contentcouch.store.ParseRdfGetFilter;
import contentcouch.store.Pusher;
import contentcouch.store.Putter;
import contentcouch.store.Sha1BlobStore;
import contentcouch.value.Blob;

public class ContentCouchRepository implements Getter, Pusher {
	protected String path;
	
	// TODO: Allow repo to be accessed remotely (http, freenet, etc) 
	protected Getter dataGetter;
	protected Pusher dataPusher;
	protected Getter headGetter;
	protected Putter headPutter;
	protected Getter exploratBlobGetter;
	
	public ContentCouchRepository( String path ) {
		this.path = path;
		Sha1BlobStore bs = new Sha1BlobStore( new FileBlobMap(path + "/data/") );
		dataGetter = bs;
		dataPusher = bs;
		FileBlobMap hs = new FileBlobMap(path + "/heads/");
		headPutter = hs;
		headGetter = hs;
		
		MultiGetter mbg = new MultiGetter();
		mbg.addGetter(new FileBlobMap(path + "/"));
		mbg.addGetter(dataGetter);
		mbg.addGetter(new HttpBlobGetter()); // Take this out and you can remove some ext-lib jars
		exploratBlobGetter = new ParseRdfGetFilter(mbg);
	}
	
	public void initialize() {
		FileUtil.mkParentDirs( new File(path) );
	}
	
	public String push( Object obj ) {
		return dataPusher.push(obj);
	}
	
	public Object get( String identifier ) {
		return exploratBlobGetter.get(identifier);
	}
	
	public void putHead( String headName, Blob headData ) {
		headPutter.put(headName, headData);
	}
	
	public void putHead( String headName, RdfNode headInfo ) {
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
