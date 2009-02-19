package contentcouch.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import contentcouch.blob.BlobUtil;
import contentcouch.file.FileUtil;
import contentcouch.hashcache.FileHashCache;
import contentcouch.http.HttpBlobGetter;
import contentcouch.rdf.RdfNode;
import contentcouch.store.FileBlobMap;
import contentcouch.store.Getter;
import contentcouch.store.ParseRdfGetFilter;
import contentcouch.store.PrefixGetFilter;
import contentcouch.store.Pusher;
import contentcouch.store.Putter;
import contentcouch.store.Sha1BlobStore;
import contentcouch.value.Blob;
import contentcouch.value.Ref;

public class ContentCouchRepository implements Getter, Pusher {
	protected String path;
	
	public Getter dataGetter;
	public Pusher dataPusher;
	public Getter headGetter;
	public Putter headPutter;
	public Getter exploratGetter;
	public boolean explorable = false;
	
	public ContentCouchRepository remoteCacheRepository; 
	public List localRepositories = new ArrayList();
	public List remoteRepositories = new ArrayList();
	
	public ContentCouchRepository() {
	}
	
	public ContentCouchRepository( String path ) {
		this();
		
		if( !path.endsWith("/") ) path += "/";
		this.path = path;		
		
		if( path.startsWith("http:") ) {
			HttpBlobGetter hbg = new HttpBlobGetter();
			
			dataGetter = new ParseRdfGetFilter(new Sha1BlobStore( new PrefixGetFilter(hbg, path + "data/"), null ));
			headGetter = new PrefixGetFilter(hbg, path + "heads/");
		} else {
			exploratGetter = new FileBlobMap(path);
			Sha1BlobStore bs = new Sha1BlobStore( new FileBlobMap(path + "data/") );
			dataGetter = new ParseRdfGetFilter(bs);
			dataPusher = bs;
			FileBlobMap hs = new FileBlobMap(path + "heads/");
			headPutter = hs;
			headGetter = hs;

			File cf = new File(path + "cache/file-attrs.slf");
			bs.fileHashCache = new FileHashCache(cf);
		}
	}
	
	public void addLocal( ContentCouchRepository repo ) {
		localRepositories.add(0,repo);
	}
	
	public void addRemote( ContentCouchRepository repo ) {
		remoteRepositories.add(0,repo);
	}

	public void initialize() {
		FileUtil.mkParentDirs( new File(path) );
	}
	
	public String push( Object obj ) {
		return dataPusher.push(obj);
	}
	
	public Object getReallyLocal( String identifier ) {
		return dataGetter.get(identifier);
	}
	
	public Object getLocal( String identifier ) {
		Object obj = getReallyLocal(identifier);
		if( obj != null ) return obj;
		
		for( Iterator i=localRepositories.iterator(); i.hasNext(); ) {
			ContentCouchRepository localRepo = (ContentCouchRepository)i.next();
			obj = localRepo.getReallyLocal(identifier);
			if( obj != null ) return obj;
		}
		
		return null;
	}
	
	public Object getRemote( String identifier ) {
		Object obj;
		for( Iterator i=remoteRepositories.iterator(); i.hasNext(); ) {
			ContentCouchRepository localRepo = (ContentCouchRepository)i.next();
			obj = localRepo.getReallyLocal(identifier);
			if( obj != null ) return obj;
		}
		return null;
	}
	
	public Object get( String identifier ) {
		Object obj;
		if( explorable && exploratGetter != null ) {
			obj = exploratGetter.get(identifier);
			if( obj != null ) return obj;
		}
		
		// Check this repo
		obj = getLocal(identifier);
		if( obj != null ) return obj;
		
		// Check cache repo
		if( remoteCacheRepository != null ) {
			obj = remoteCacheRepository.getReallyLocal(identifier);
			if( obj != null ) return obj;
		}
		
		// Check remote repos
		obj = getRemote(identifier);
		if( obj != null && remoteCacheRepository != null ) {
			String cachedId = remoteCacheRepository.push(obj);
			if( cachedId == null || !(cachedId.equals(identifier)) ) {
				throw new RuntimeException("Calculated identifier (" + cachedId + ") does not match requested identifier (" + identifier + ")");
			}
		}
	
		return obj;
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
	
	public static void main(String[] args) {
		ContentCouchRepository mainRepo = new ContentCouchRepository("junk/main-repo");
		mainRepo.remoteCacheRepository = new ContentCouchRepository("junk/cache-repo");
		mainRepo.addRemote(new ContentCouchRepository("http://localhost/ccouch/"));
		
		String uri = "urn:sha1:2AAQ5X4YWKVE3HRKWXTGID6QOABFSRWD";
		
		Blob b = (Blob)mainRepo.get(uri);
		if( b == null ) {
			System.err.println("Couldn't find " + uri);
		} else {
			BlobUtil.writeBlobToFile(b, new File("junk/windowpotts.jpg") );
		}
		
		Exporter e = new Exporter(mainRepo);
		e.exportObject(new Ref("x-parse-rdf:urn:sha1:AMQT7PP6HTDP5WMH6RTW3RWIDR5JTXNK"), new File("junk/2009-01-15-pix") );
	}
}
