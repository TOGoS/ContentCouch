package contentcouch.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import contentcouch.file.FileDirectory;
import contentcouch.file.FileUtil;
import contentcouch.hashcache.FileHashCache;
import contentcouch.http.HttpBlobGetter;
import contentcouch.path.PathUtil;
import contentcouch.store.FileBlobMap;
import contentcouch.store.Getter;
import contentcouch.store.Identifier;
import contentcouch.store.PrefixGetFilter;
import contentcouch.store.Pusher;
import contentcouch.store.Putter;
import contentcouch.store.Sha1BlobStore;
import contentcouch.store.StoreFileGetter;
import contentcouch.value.Blob;
import contentcouch.value.Directory;

public class ContentCouchRepository implements Getter, Pusher, Identifier, StoreFileGetter {
	protected static class RepoParameters {
		public static final String DISPOSITION_MAIN = "main";
		public static final String DISPOSITION_LOCAL = "local";
		public static final String DISPOSITION_CACHE = "cache";
		public static final String DISPOSITION_REMOTE = "remote";
		
		public String disposition;
		public String name;
		
		public static RepoParameters parse(String arg) {
			String[] parts = arg.split(":");
			
			String disposition;
			if( "-repo".equals(parts[0]) ) {
				disposition = DISPOSITION_MAIN;
			} else if( "-remote-repo".equals(parts[0]) ) {
				disposition = DISPOSITION_REMOTE;
			} else if( "-cache-repo".equals(parts[0]) ) {
				disposition = DISPOSITION_CACHE;
			} else if( "-local-repo".equals(parts[0]) ) {
				disposition = DISPOSITION_LOCAL;
			} else {
				return null;
			}
			
			RepoParameters rp = new RepoParameters();
			rp.disposition = disposition;
			if( parts.length >= 2 ) {
				rp.name = parts[1];
			}
			return rp;
		}
	}
	
	protected String path;
	
	public Getter dataGetter;
	public Pusher dataPusher;
	public Getter headGetter;
	public Putter headPutter;
	public Getter exploratGetter;
	public Identifier identifier;
	public boolean initialized = false;
	public boolean explorable = false;
	public boolean isMainRepo = false;
	public String name = "unnamed";
	
	public ContentCouchRepository remoteCacheRepository; 
	public List localRepositories = new ArrayList();
	public List remoteRepositories = new ArrayList();
	
	public Map namedRepositories = new HashMap();
	
	public Map cmdArgs = new HashMap();
	
	public ContentCouchRepository() {
	}
	
	public ContentCouchRepository( String path, boolean isMainRepo ) {
		this();
		this.isMainRepo = isMainRepo;
		if( path == null ) return;
		try {
			loadConfigAt(path);
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public ContentCouchRepository( String path ) {
		this( path, true );
	}
	
	//// Configuration ////

	public void initBasics( String path ) throws IOException {
		if( !path.endsWith("/") ) path += "/";
		this.path = path;		
		
		if( path.startsWith("http:") ) {
			HttpBlobGetter hbg = new HttpBlobGetter();
			
			dataGetter = new Sha1BlobStore( new PrefixGetFilter(hbg, path + "data/"), null );
			headGetter = new PrefixGetFilter(hbg, path + "heads/");
		} else {
			if( FileUtil.mkParentDirs( new File(path) ) ) {
				File configFile = new File(path + "/ccouch-config");
				FileWriter fw = new FileWriter(configFile);
				fw.write("# This is the config file for this repository.\n");
				fw.write("# Add options here as you would specify them on the command line.\n");
				fw.write("# For example:\n");
				fw.write("# -remote-repo http://www.example.com/r3p0/\n");
				fw.write("# -use-main-repo-as-cache\n");
				fw.close();
			}

			exploratGetter = new FileBlobMap(path);
			Sha1BlobStore bs = new Sha1BlobStore( new FileBlobMap(path + "data/") );
			dataGetter = bs;
			dataPusher = bs;
			identifier = bs;
			FileBlobMap hs = new FileBlobMap(path + "heads/");
			headPutter = hs;
			headGetter = hs;

			File cf = new File(path + "cache/file-attrs.slf");
			bs.fileHashCache = new FileHashCache(cf);
		}
		_loadConfig( new File(path + "/ccouch-config") );
		
		initialized = true;
	}
	
	// Used to interpret relative paths in arguments
	public String basePath;
	
	/** Interprets arguments at the given offset in the given argument array
	 * that are understood and returns the offset into the arguments list
	 * after the parsed arguments. If the offset given is returned, then it
	 * can be assumed that no arguments were understood. */
	public int handleArguments( String[] args, int offset ) {
		if( offset >= args.length ) return offset;
		String arg = args[offset];
		RepoParameters rp = RepoParameters.parse(arg);
		if( rp != null ) {
			++offset;
			String path = PathUtil.appendPath(basePath, args[offset]);
			++offset;
			if( isMainRepo ) {
				ContentCouchRepository repo;
				if( rp.disposition == RepoParameters.DISPOSITION_MAIN ) {
					try {
						initBasics(path);
					} catch( IOException e ) {
						throw new RuntimeException("Couldn't initialize repo at " + path, e);
					}
					if( rp.name == null ) rp.name = "main";
					repo = this;
				} else if( rp.disposition == RepoParameters.DISPOSITION_LOCAL ) {
					repo = new ContentCouchRepository(path, false);
					addLocal(repo);
				} else if( rp.disposition == RepoParameters.DISPOSITION_CACHE ) {
					repo = new ContentCouchRepository(path, false);
					remoteCacheRepository = repo;
				} else if( rp.disposition == RepoParameters.DISPOSITION_REMOTE ) {
					repo = new ContentCouchRepository(path, false);
					addRemote(repo);
				} else {
					throw new RuntimeException("unknown repo disposition: " + rp.disposition);
				}
				if( explorable ) repo.explorable = true;
				if( rp.name != null ) {
					namedRepositories.put(rp.name, repo);
					repo.name = rp.name;
				}
			}
		}
		return offset;
	}
	
	static Pattern argPattern = Pattern.compile("(?:\\S|\"(?:[^\\\\\"]|\\\\\\\\|\\\\\")*\")+");
	
	protected static String unescape(String arg) {
		StringBuffer res = new StringBuffer();
		for( int i=0; i<arg.length(); ++i ) {
			char c = arg.charAt(i);
			if( c == '\\' && i<arg.length()-1 ) {
				++i;
				switch(c = arg.charAt(i)) {
				case('n'): c = '\n'; break;
				case('t'): c = '\t'; break;
				case('r'): c = '\r'; break;
				}
			}
			res.append(c);
		}
		return res.toString();
	}
	
	protected void _loadConfig( BufferedReader fr, String sourceLocation ) throws IOException {
		String oldBasePath = basePath;
		try {
			basePath = sourceLocation;
			
			ArrayList args = new ArrayList();
			String line;
			String cmdName = null;
			lines: while( (line = fr.readLine()) != null ) {
				Matcher m = argPattern.matcher(line);
				while( m.find() ) {
					String arg = m.group();
					if( arg.charAt(0) == '#' ) continue lines;
					if( arg.charAt(0) == '[' ) {
						cmdName = arg.substring(1,arg.length()-1);
						continue;
					}
					if( arg.charAt(0) == '"' ) arg = unescape(arg.substring(1,arg.length()-1));
					if( cmdName == null ) {
						args.add(arg);
					} else {
						List cas = (List)cmdArgs.get(cmdName);
						if( cas == null ) cmdArgs.put(cmdName, cas = new ArrayList());
						cas.add(arg);
					}
				}
			}
			String[] argar = new String[args.size()];
			argar = (String[])args.toArray(argar);
			int endupat;
			int offset = 0;
			while( (endupat = handleArguments( argar, offset )) > offset ) {
			    offset = endupat;
			}
			if( endupat < argar.length ) {
				System.err.println("Unrecognised arg in " + sourceLocation + ": " + argar[endupat]);
			}
		} finally {
			basePath = oldBasePath;
		}
	}
	
	public String[] getCommandArgs(String commandName) {
		List l = (List)cmdArgs.get(commandName);
		if( l == null ) return new String[0];
		String[] s = new String[l.size()];
		return (String[])l.toArray(s);
	}

	protected void _loadConfig( File f ) throws IOException {
		BufferedReader fr;
		try {
			fr = new BufferedReader(new FileReader(f));
			_loadConfig(fr, f.getPath());
			fr.close();
		} catch (FileNotFoundException e) {
			return;
		}
	}
	
	public void loadConfig( File f ) throws IOException {
		if( f.exists() && f.isFile() ) {
			_loadConfig( f );
		} else {
			initBasics(f.getPath());
		}
	}

	public void loadConfigAt( String path ) throws IOException {
		File f = new File(path);
		if( f.exists() && f.isFile() ) {
			_loadConfig( f );
		} else {
			initBasics(path);
		}
	}
	
	public void addLocal( ContentCouchRepository repo ) {
		localRepositories.add(0,repo);
	}
	
	public void addRemote( ContentCouchRepository repo ) {
		remoteRepositories.add(0,repo);
	}

	//// Get stuff ////
	
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
			obj = remoteCacheRepository.get(cachedId);
		}
	
		return obj;
	}
	
	public String identify( Object obj ) {
		if( identifier != null ) return identifier.identify(obj);
		return null;
	}
	
	public File getStoreFile(String identifier) {
		if( dataGetter instanceof StoreFileGetter ) {
			return ((StoreFileGetter)dataGetter).getStoreFile(identifier);
		} else {
			return null;
		}
	}

	public File getStoreFile(Blob blob) {
		if( dataPusher instanceof StoreFileGetter ) {
			return ((StoreFileGetter)dataPusher).getStoreFile(blob);
		} else {
			return null;
		}
	}
	
	public Directory getDirectory() {
		return new FileDirectory(new File(path));
	}

	//// Put stuff ////
	
	public String push( Object obj ) {
		return dataPusher.push(obj);
	}
}
