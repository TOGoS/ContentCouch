package contentcouch.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.eekboom.utils.Strings;

import contentcouch.active.ActiveUriResolver;
import contentcouch.active.DataUriResolver;
import contentcouch.app.Log;
import contentcouch.directory.DirectoryUtil;
import contentcouch.file.FileUtil;
import contentcouch.hashcache.FileHashCache;
import contentcouch.http.HtmlDirectoryGetFilter;
import contentcouch.http.HttpBlobGetter;
import contentcouch.path.PathUtil;
import contentcouch.rdf.RdfNamespace;
import contentcouch.store.BlobStore;
import contentcouch.store.FileBlobMap;
import contentcouch.store.Getter;
import contentcouch.store.Identifier;
import contentcouch.store.MultiGetter;
import contentcouch.store.ParseRdfGetFilter;
import contentcouch.store.PrefixGetFilter;
import contentcouch.store.Pusher;
import contentcouch.store.Putter;
import contentcouch.store.Sha1BlobStore;
import contentcouch.store.StoreFileGetter;
import contentcouch.store.TheGetter;
import contentcouch.store.TheIdentifier;
import contentcouch.value.Blob;
import contentcouch.value.Directory;

public class ContentCouchRepository implements Getter {
	public static class DownloadInfo {
		public ContentCouchRepository sourceRepo;
		public String sourceUrl;
		public long dataLength;
		
		public DownloadInfo(ContentCouchRepository sourceRepo, Object dlo) {
			this.sourceRepo = sourceRepo;
			if( dlo instanceof Blob ) {
				dataLength = ((Blob)dlo).getLength();
			}
		}
		
		public String getRepoName() {
			if( sourceRepo == null || sourceRepo.name == null ) {
				return "(unnamed repo)";
			} else {
				return "'" + sourceRepo.name + "'";
			}
		}
		
		public String getShortDesc() {
			String d = " from " + getRepoName();
			if( dataLength != -1 ) {
				d += ", " + dataLength + " bytes";
			}
			return d;
		}

		public String getLengthDesc() {
			if( dataLength != -1 ) {
				return " " + dataLength + "B";
			} else {
				return "";
			}
		}
	}
	
	public static interface GetAttemptListener {
		public static int GET_FAILED      = 1;
		public static int GOT_FROM_REMOTE = 2;
		public static int GOT_FROM_CACHE  = 3;
		public static int GOT_FROM_LOCAL  = 4;
		
		public void getAttempted( String uri, int status, DownloadInfo info );
	}
	
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
	
	public Pusher dataPusher;
	public Getter headGetter;
	public Putter headPutter;
	public Getter exploratGetter;
	public Identifier identifier;
	public boolean initialized = false;
	public boolean isMainRepo = false;
	public String name = "unnamed";
	public String cacheSector = "remote";
	
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
	
	public void registerAsGetterAndIdentifier() {
		TheGetter.globalInstance = getGenericGetter();
		TheIdentifier.globalInstance = new Sha1BlobStore(null, null);
	}
	
	public String getPath() {
		return path;
	}
	
	public Identifier getBlobIdentifier() {
		return new Sha1BlobStore(null, null);
	}
	
	public void writeDefaultConfig(File configFile) throws IOException {
		String resname = "config-template.txt";
		InputStream res = this.getClass().getResourceAsStream(resname);
		if( res == null ) throw new IOException("Couldn't find internal resource: " + this.getClass().getPackage().getName() + "/" + resname);
		FileWriter fw = new FileWriter(configFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(res));
		String line;
		while( (line = br.readLine()) != null ) {
			fw.write(line + "\n");
		}
		br.close();
		fw.close();
	}

	public void initBasics( String path ) throws IOException {
		if( !path.endsWith("/") ) path += "/";
		this.path = path;
		
		if( path.startsWith("http:") ) {
			exploratGetter = new PrefixGetFilter(new HtmlDirectoryGetFilter(new HttpBlobGetter()), path);
			//blobStore = new Sha1BlobStore( new PrefixGetFilter(exploratGetter, "data/"), null );
			headGetter = new PrefixGetFilter(exploratGetter, "heads/");
		} else {
			FileUtil.mkdirs( new File(path) );
			File configFile = new File(path + "/ccouch-config");
			if( !configFile.exists() ) {
				try {
					writeDefaultConfig(configFile);
				} catch( IOException e ) {
					System.err.println("Failed to write default config file at " + configFile.getPath() + ": " + e.getMessage());
				}
			}

			exploratGetter = new FileBlobMap(path + "/");
			Sha1BlobStore bs = new Sha1BlobStore( new FileBlobMap(path + "data/") );
			//blobStore = bs;
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
			//System.err.println(basePath + " + " + args[offset] + " = " + path);
			++offset;
			if( isMainRepo ) {
				ContentCouchRepository repo;
				if( rp.disposition == RepoParameters.DISPOSITION_MAIN ) {
					try {
						initBasics(path);
					} catch( IOException e ) {
						throw new RuntimeException("Couldn't initialize repo at " + path, e);
					}
					repo = this;
				} else if( rp.disposition == RepoParameters.DISPOSITION_LOCAL ) {
					repo = new ContentCouchRepository(path, false);
					addLocal(repo);
				} else if( rp.disposition == RepoParameters.DISPOSITION_CACHE ) {
					Log.log(Log.LEVEL_WARNINGS, "Cache repo specified, but cache repositories are no longer used.  Treating as a local repo.");
					repo = new ContentCouchRepository(path, false);
					addLocal(repo);
				} else if( rp.disposition == RepoParameters.DISPOSITION_REMOTE ) {
					repo = new ContentCouchRepository(path, false);
					addRemote(repo);
				} else {
					throw new RuntimeException("unknown repo disposition: " + rp.disposition);
				}
				if( rp.name != null ) repo.name = rp.name;
				if( repo.name != null ) namedRepositories.put(repo.name, repo);
			}
		} else if( "-repo-name".equals(arg) ) {
			++offset;
			String name = args[offset];
			++offset;
			this.name = name;
			namedRepositories.put(name, this);
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

	//// Get listeners ////
	
	protected Set getAttemptListeners;
	
	public void addGetAttemptListener( GetAttemptListener l ) {
		if( getAttemptListeners == null ) getAttemptListeners = new HashSet();
		getAttemptListeners.add(l);
	}
	
	public boolean removeGetAttemptListener( GetAttemptListener l ) {
		if( getAttemptListeners == null ) return false;
		return getAttemptListeners.remove(l);
	}

	public void getAttempted( String uri, int status, DownloadInfo info ) {
		if( getAttemptListeners == null ) return;
		for( Iterator i=getAttemptListeners.iterator(); i.hasNext(); ) {
			((GetAttemptListener)i.next()).getAttempted(uri, status, info);
		}
	}
	
	public void getAttempted( String uri, int status, ContentCouchRepository foundAt, Object o ) {
		getAttempted(uri, status, new DownloadInfo(foundAt, o));
	}

	//// Get stuff ////

	protected BlobStore getBlobStoreAt( String uri ) {
		return new Sha1BlobStore( uri );
	}
	
	public BlobStore getBlobStore( String name ) {
		return getBlobStoreAt( this.path + "data/" + name + "/" ); 	
	}
	
	public Object getReallyLocal( String identifier ) {
		String dataDirPath = this.path + "data/";
		// TODO: Use new getter arch. to get actual metadata instead of empty map
		Directory dataDir = DirectoryUtil.getDirectory(TheGetter.get(dataDirPath), Collections.EMPTY_MAP, dataDirPath);
		if( dataDir == null ) {
			Log.log(Log.LEVEL_WARNINGS, "Could not load dir " + dataDirPath);
			return null;
		}
		for( Iterator i=dataDir.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry e = (Directory.Entry)i.next();
			if( RdfNamespace.OBJECT_TYPE_DIRECTORY.equals(e.getTargetType()) ) {
				String bsUri = dataDirPath + e.getKey() + "/";
				Log.log(Log.LEVEL_CHATTIER, "Looking for " + identifier + " under " + bsUri);
				Object obj = getBlobStoreAt(bsUri).get(identifier);
				if( obj != null ) {
					getAttempted( identifier, GetAttemptListener.GOT_FROM_LOCAL, this, obj );
					return obj;
				}
			}
		}
		return null;
	}
	
	public Object getLocal( String identifier ) {
		Object obj = getReallyLocal(identifier);
		if( obj != null ) return obj;
		
		for( Iterator i=localRepositories.iterator(); i.hasNext(); ) {
			ContentCouchRepository localRepo = (ContentCouchRepository)i.next();
			obj = localRepo.getReallyLocal(identifier);
			if( obj != null ) {
				getAttempted( identifier, GetAttemptListener.GOT_FROM_LOCAL, localRepo, obj );
				return obj;
			}
		}
		
		return null;
	}
	
	public Object getRemote( String identifier, boolean cache ) {
		Object obj;
		for( Iterator i=remoteRepositories.iterator(); i.hasNext(); ) {
			ContentCouchRepository remoteRepo = (ContentCouchRepository)i.next();
			obj = remoteRepo.getReallyLocal(identifier);
			if( obj != null ) {
				getAttempted( identifier, GetAttemptListener.GOT_FROM_REMOTE, remoteRepo, obj );
				if( cache ) {
					String cachedId = cache((Blob)obj);
					if( cachedId == null || !(cachedId.equals(identifier)) ) {
						obj = null;
						Log.log(Log.LEVEL_WARNINGS, "Calculated identifier (" + cachedId + ") does not match requested identifier (" + identifier + "), downloaded from " + remoteRepo.path);
					} else {
						obj = getReallyLocal(cachedId);
					}
				}
				if( obj != null ) return obj;
			}
		}
		return null;
	}
	
	public String cache( Blob blob ) {
		return getBlobStore(cacheSector).push(blob);
	}
	
	public Object getExplorat( String identifier ) {
		Object o = exploratGetter.get(identifier);
		return o;
	}
	
	public CCouchHeadGetter getHeadGetter( boolean checkRemotes ) {
		CCouchHeadGetter g = new CCouchHeadGetter(this);
		g.checkRemotes = checkRemotes;
		return g;
	}
	
	public ActiveUriResolver getActiveUriResolver( Getter getter ) {
		ActiveUriResolver aur = new ActiveUriResolver( getter );

		return aur;
	}
	
	public Getter getGenericGetter() {
		MultiGetter mg = new MultiGetter();
		mg.addGetter(this);
		mg.addGetter(getHeadGetter(false));
		mg.addGetter(new FileBlobMap(""));
		mg.addGetter(new ParseRdfGetFilter(mg, false));
		mg.addGetter(getActiveUriResolver(mg));
		mg.addGetter(new DataUriResolver());
		mg.addGetter(new HttpBlobGetter());
		return mg;
	}
	
	public Object get( String identifier ) {
		if( !identifier.startsWith("urn:sha1:") ) {
			// Not the repository's job to get anything else.
			return null;
		}
		
		Object obj;
		
		// Check this repo
		obj = getLocal(identifier);
		if( obj != null ) return obj;
		
		// Check remote repos
		obj = getRemote(identifier, true);
	
		if( obj == null ) getAttempted( identifier, GetAttemptListener.GET_FAILED, null );
		
		return obj;
	}
	
	public String identify( Object obj ) {
		if( identifier != null ) return identifier.identify(obj);
		return null;
	}
	
	public String identifyAt( String uri ) {
		if( identifier != null ) return identifier.identifyAt( uri );
		return null;
	}

	public File getStoreFile(Blob blob) {
		if( dataPusher instanceof StoreFileGetter ) {
			return ((StoreFileGetter)dataPusher).getStoreFile(blob);
		} else {
			return null;
		}
	}
	
	/** If path ends with /latest, finds and returns the path with the newest version */
	public String findHead(String path) {
		if( path.endsWith("/latest") ) {
			String dirPath = path.substring(0,path.length()-"latest".length());
			Object dir = exploratGetter.get("heads/"+dirPath);
			if( dir instanceof Directory ) {
				String highestKey = null;
				for( Iterator i=((Directory)dir).getDirectoryEntrySet().iterator(); i.hasNext(); ) {
					Directory.Entry e = (Directory.Entry)i.next();
					String k = e.getKey();
					if( highestKey == null || Strings.compareNatural(k,highestKey) > 0 ) highestKey = k;
				}
				if( highestKey != null ) {
					return dirPath+highestKey;
				}
			}
			return null;
		} else {
			return path;
		}
	}
	
	public Object getHead(String path) {
		Object res = exploratGetter.get("heads/"+path);
		if( res == null && path.endsWith("/latest") ) {
			path = findHead(path);
			if( path == null ) return null;
			return exploratGetter.get("heads/" + path);
		}
		return res;
	}
}
