// -*- tab-width:4 -*-
package contentcouch.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import contentcouch.blob.BlobUtil;
import contentcouch.file.FileDirectory;
import contentcouch.rdf.RdfIO;
import contentcouch.rdf.RdfNamespace;
import contentcouch.rdf.RdfNode;
import contentcouch.repository.CCouchHeadGetter;
import contentcouch.repository.ContentCouchRepository;
import contentcouch.repository.ContentCouchRepository.DownloadInfo;
import contentcouch.repository.ContentCouchRepository.GetAttemptListener;
import contentcouch.store.FileBlobMap;
import contentcouch.store.Getter;
import contentcouch.store.MultiGetter;
import contentcouch.store.ParseRdfGetFilter;
import contentcouch.value.Blob;
import contentcouch.value.Commit;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class ContentCouchCommand {
	public String USAGE =
		"Usage: ccouch [general options] <sub-command> [command-args]\n" +
		"Run ccouch <subcommand> -? for further help\n" +
		"General options:\n" +
		"  -repo <path>          ; specify main repository\n" +
		"  -local-repo <path>    ; specify secondary local repository\n" +
		"  -cache-repo <path>    ; specify repository to cache downloaded objects\n" +
		"  -remote-repo <path>   ; specify a remote repository\n" +
		"Sub-commands:\n" +
		"  store <files>         ; store files in the repo\n" +
		"  checkout <uri> <dest> ; check files out to the filesystem\n" +
		"  cache <urn> ...       ; cache blobs\n" +
		"  cache-heads ...       ; cache heads from another repository\n" +
		"  id <files>            ; give URNs for files without storing\n" +
		"  rdfify <dir>          ; print RDF listing of a directory\n" +
		"  check                 ; check repo integrity and delete bad files";
	
	public String STORE_USAGE =
		"Usage: ccouch [general options] store [store options] <file1> <file2> ...\n" +
		"Store options:\n" +
		"  -m <message>       ; create a commit with this message\n" +
		"  -a <author>        ; create a commit with this author\n" +
		"  -n <name>          ; name your commit this\n" +
		"  -force-commit      ; create a new commit even if nothing has changed\n" +
		"  -link              ; hardlink files into the store instead of copying\n" +
		"  -files-only        ; store only file content (no directory listings)\n" +
		"  -dirs-only         ; store only directory listings (no file content)\n" +
		"  -dont-store        ; store nothing (same as using 'ccocuch id')\n" +
		"  -relink            ; hardlink imported files to their stored counterpart\n" +
		"  -v                 ; verbose - report every path -> urn mapping\n" +
		"  -q                 ; quiet - show nothing\n" +
		"  -?                 ; display help and exit\n" +
		"\n" +
		"If -m, -a, and/or -n are used, a commit will be created and its URN output.\n" +
		"\n" +
		"If -n is specified, a commit will be stored under that name as\n" +
		"<repo-path>/heads/<main-repo-name>/<name>/<version>, where <version> is automatically\n" +
		"incremented for new commits.\n" +
		"\n" +
		"-relink is useful when a copy of the file is already in the\n" +
		"repository and you want to make sure the data ends up being\n" +
		"shared.  -relink implies -link.";
	
	public String CHECKOUT_USAGE =
		"Usage: ccouch [general options] checkout [checkout options] <source> <dest>\n" +
		"Checkout options:\n" +
		"  -link              ; hardlink files from the store instead of copying\n" +
		"  -merge             ; merge source tree into destination\n" +
		"  -replace-existing  ; when merging, always replace existing files\n" +
		"  -keep-existing     ; when merging, always keep existing files\n" +
		"  -dirs-only         ; only export the directory structure\n" +
		"  -v                 ; verbose - report every file visited\n" +
		"  -?                 ; display help and exit\n" +
		"\n" +
		"When merging, unless -replace-existing is given:\n" +
		"- only files that do not already exist in the destination folder will be\n" +
		"  checked out.\n" +
		"- If the destination file has the same content as the to-be-checked-out file,\n" +
		"  no action is taken.\n" +
		"- If the content is different, an error is printed and the program exits.";
		
	public static String CACHE_USAGE =
		"Usage: ccouch [general options] cache [options] <urn> <urn> ...\n" +
		"Options:\n" +
		"  -v  ; show all URNs being followed\n" +
		"  -q  ; show nothing - not even failures\n" +
		"\n" +
		"Attempts to cache any objects that are not already in a local repository\n" +
		"into your cache repository.  Directories, Commits, and Redirects will\n" +
		"be followed and all referenced objects will be cached.\n" +
		"\n" +
		"By default, URIs that fail to load and ones that are newly cached are\n" +
		"reported to stderr\n";
	
	public static String CACHE_HEADS_USAGE =
		"Usage: ccouch [general options] cache-heads [options] <head-uri> ...\n" +
		"Options:\n" +
		"  -v           ; show all exports and skipped files\n" +
		"  -q           ; show only failures\n" +
		"  -all-remotes ; cache heads from each remote repository\n" +
		"\n" +
		"Attempts to cache heads from the given repo/paths into your cache repository.\n" +
		"\n" +
		"If -all-remotes is given, heads from each remote repository under a folder\n" +
		"with the same name as the repository will be stored in the cache repository\n" +
		"under that same name.  e.g.\n" +
		"  x-ccouch-repo://barney-repo/barney-repo/foobar/123 will be cached at\n" +
		"  x-ccouch-repo://my-cache-repo/barney-repo/foobar/123\n" +
		"\n" +
		"Otherwise, head-uri can be of any of the following forms:\n" +
		"  //repo/path/  ; cache only a certain set of heads from a certain repository\n" +
		"  /path/        ; cache heads under the given path from any repository that\n" +
		"                ; has them";
	
	public static String RDFIFY_USAGE =
		"Usage: ccouch [general options] rdfify [rdfify options] <dir>\n" +
		"Rdfify options:\n" +
		"  -nested            ; nest sub-dirs in output instead of linking to them";

	public static String CACHE_CHECK_USAGE =
		"Usage: ccouch [general options] check <path> <path> ...\n" +
		"\n" +
		"Walks the named directories or files and ensures that all non-dot files'\n" +
		"names match their base-32 encoded SHA-1 hash.\n" +
		"\n" +
		"If no paths are given, checks the data directory in the main repository";
	
	////
	
	protected ContentCouchRepository repositoryCache = null;
	protected int cacheVerbosity = GetAttemptListener.GOT_FROM_REMOTE;
	
	public ContentCouchRepository getRepository() {
		if( repositoryCache == null ) {
			repositoryCache = new ContentCouchRepository(null, true);
			repositoryCache.addGetAttemptListener(new GetAttemptListener() {
				public void getAttempted( String uri, int status, DownloadInfo info ) {
					if( cacheVerbosity >= status ) {
						switch( status ) {
						case( 1 ): System.err.println("! Couldn't find  " + uri); break;
						case( 2 ): System.err.println("  Downloading    " + uri + info.getShortDesc() ); break;
						case( 3 ): System.err.println("  Already cached " + uri); break;
						case( 4 ): System.err.println("  Already local  " + uri); break;
						}
					}
				}
			});
		}
		return repositoryCache;
	}
		
	public Importer getImporter() {
		return new Importer(getRepository());
	}
	
	public CCouchHeadGetter getHeadGetter( boolean checkRemotes ) {
		CCouchHeadGetter g = new CCouchHeadGetter(getRepository());
		g.checkRemotes = checkRemotes;
		return g;
	}
	
	public Getter getLocalGetter() {
		MultiGetter mg = new MultiGetter();
		mg.addGetter(getRepository());
		mg.addGetter(getHeadGetter(false));
		mg.addGetter(new FileBlobMap(""));
		return new ParseRdfGetFilter(mg);
	}
	
	protected String[] concat( String[] s1, String[] s2 ) {
		String[] r = new String[s1.length+s2.length];
		int j = 0;
		for( int i=0; i<s1.length; ++i, ++j ) r[j] = s1[i];
		for( int i=0; i<s2.length; ++i, ++j ) r[j] = s2[i];
		return r;
	}
	
	protected String[] mergeConfiguredArgs( String commandName, String[] commandLineArgs ) {
		return concat( getRepository().getCommandArgs(commandName), commandLineArgs );
	}
	
	//// Commit tracking ////
	
	public File getParentCommitListFile(File about) {
		if( about.isDirectory() ) {
			return new File(about + "/.ccouch-commit-uris");
		} else {
			return null;
		}
	}
	
	public String[] readUris(BufferedReader r) throws IOException {
		ArrayList commitUris = new ArrayList();
		String line;
		while( (line = r.readLine()) != null ) {
			line = line.trim();
			if( line.startsWith("#") ) continue;
			commitUris.add(line);
		}
		String[] commitUriArr = new String[commitUris.size()];
		return (String[])commitUris.toArray(commitUriArr);
	}
	
	public String[] getParentCommitUris(File about) {
		try {
			File parentCommitListFile = getParentCommitListFile(about);
			if( parentCommitListFile == null || !parentCommitListFile.exists() ) return new String[0];
			BufferedReader r = new BufferedReader(new FileReader(parentCommitListFile));
			try {
				return readUris(r);
			} finally {
				r.close();
			}
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	protected void writeParentCommitUri(File about, String commitUri, boolean append) {
		String[] existingCommitUris = getParentCommitUris(about);
		if( existingCommitUris != null ) for( int i=0; i<existingCommitUris.length; ++i ) {
			if( existingCommitUris[i].equals(commitUri) ) return;
		}
		
		File commitUriListFile = getParentCommitListFile(about);
		if( commitUriListFile == null ) return;
		try {
			FileWriter w = new FileWriter(commitUriListFile, append);
			try {
				w.write(commitUri);
				w.write("\n");
			} finally {
				w.close();
			}
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public void addParentCommitUri(File about, String commitUri) {
		writeParentCommitUri(about, commitUri, true);
	}
	
	public void setParentCommitUri(File about, String commitUri) {
		writeParentCommitUri(about, commitUri, false);	
	}
	
	//// Commands ////
	
	public void runStoreCmd( String[] args ) {
		args = mergeConfiguredArgs("store", args);
		List files = new ArrayList();
		String message = null;
		String name = null;
		String author = null;
		final Importer importer = getImporter();
		int verbosity = 1;
		boolean storeFiles = true;
		boolean storeDirs = true;
		boolean storeCommits = true;
		boolean forceCommit = false;
		for( int i=0; i < args.length; ++i ) {
			String arg = args[i];
			if( arg.length() == 0 ) {
				System.err.println(STORE_USAGE);
				System.exit(1);
			} else if( "-v".equals(arg) ) {
				verbosity = 2;
			} else if( arg.startsWith("-v") ) {
				verbosity = Integer.parseInt(arg.substring(2));
			} else if( "-dont-store".equals(arg) ) {
				storeFiles = false;
				storeDirs = false;
				storeCommits = false;
			} else if( "-dont-store-files".equals(arg) ) {
				storeFiles = false;
			} else if( "-dont-store-dirs".equals(arg) ) {
				storeDirs = false;
			} else if( "-files-only".equals(arg) ) {
				storeFiles = true;
				storeDirs = false;
			} else if( "-dirs-only".equals(arg) ) {
				storeFiles = false;
				storeDirs = true;
			} else if( "-link".equals(arg) ) {
				importer.shouldLinkStored = true;
			} else if( "-relink".equals(arg) ) {
				importer.shouldLinkStored = true;
				importer.shouldRelinkImported = true;
			} else if( "-m".equals(arg) ) {
				message = args[++i];
			} else if( "-n".equals(arg) ) {
				name = args[++i];
			} else if( "-a".equals(arg) ) {
				author = args[++i];
			} else if( "-force-commit".equals(arg) ) {
				forceCommit = true;
			} else if( "-h".equals(arg) || "-?".equals(arg) ) {
				System.out.println(STORE_USAGE);
				System.exit(0);
			} else if( arg.charAt(0) != '-' ) {
				files.add(arg);
			} else {
				System.err.println("ccouch store: Unrecognised argument: " + arg);
				System.err.println(STORE_USAGE);
				System.exit(1);
			}
		}
		
		final boolean showIntermediateFiles;
		final boolean showIntermediateDirs;
		if( verbosity >= 3 ) {
			showIntermediateFiles = true;
			showIntermediateDirs = true;
		} else if( verbosity >= 2 ) {
			showIntermediateFiles = storeFiles;
			showIntermediateDirs = storeDirs;
		} else {
			showIntermediateFiles = false;
			showIntermediateDirs = false;			
		}
		
		if( files.size() == 0 ) {
			System.err.println("ccouch store: No files given");
			System.err.println(STORE_USAGE);
			System.exit(1);
		}
		boolean createCommit;
		if( name != null || message != null || author != null ) {
			createCommit = true;
			if( files.size() != 1 ) {
				System.err.println("Cannot use -m or -n with more than one file");
				System.exit(1);
			}
		} else {
			createCommit = false;
		}
		
		importer.importListener = new ImportListener() {
			public void objectImported(Object obj, String urn) {
				String uri;
				if( obj instanceof File ) {
					uri = importer.getFileUri((File)obj);
					if( ((File)obj).isDirectory() ) {
						if( !showIntermediateDirs ) uri = null;
					} else {
						if( !showIntermediateFiles ) uri = null;
					}
				} else {
					uri = "??";
					if( obj instanceof Directory ) {
						if( !showIntermediateDirs ) uri = null;
					} else {
						if( !showIntermediateFiles ) uri = null;
					}
				}
				if( uri != null ) System.out.println(uri + "\t" + urn);
			}
		};
		
		importer.shouldStoreFiles = storeFiles;
		importer.shouldStoreDirs = storeDirs;
		importer.shouldStoreHeads = storeCommits;
		
		Getter lg = getLocalGetter();
		for( Iterator i=files.iterator(); i.hasNext(); ) {
			String uri = (String)i.next();
			Object o = lg.get(uri);
			if( o == null ) throw new RuntimeException("Couldn't find " + uri);
			Ref ref = importer.importObject(o);
			
			boolean showFinal;
			if( o instanceof Directory ) {
				showFinal = !showIntermediateDirs;
			} else {
				showFinal = !showIntermediateFiles;
			}
			
			if( showFinal && verbosity > 0 ) {
				System.out.println(uri + "\t" + ref.targetUri);
			}
			if( createCommit ) {
				String targetType;
				if( ref.targetUri.charAt(0) == '@' || ref.targetUri.startsWith(RdfNamespace.URI_PARSE_PREFIX) ) {
					targetType = RdfNamespace.OBJECT_TYPE_DIRECTORY;
				} else {
					targetType = RdfNamespace.OBJECT_TYPE_BLOB;
				}
				if( name != null ) name = getRepository().name + "/" + name;
				
				String[] parentCommitUris;
				if( o instanceof File ) {
					parentCommitUris = getParentCommitUris((File)o);
					if(parentCommitUris == null) throw new RuntimeException("getParentCommitUris returned null.  Not too cool.");
				} else {
					parentCommitUris = new String[0];
				}
				
				if( parentCommitUris.length == 1 && !forceCommit ) {
					// Cancel the commit if the old one points to the same thing
					Commit oldCommit = (Commit)getLocalGetter().get(parentCommitUris[0]);
					if( oldCommit == null ) {
						System.err.println("Error: Could not load old commit " + parentCommitUris[0]);
						System.exit(1);
					}
					Object oldTarget = oldCommit.getTarget();
					if( oldTarget instanceof Ref ) {
						if( ((Ref)oldTarget).targetUri.equals(ref.targetUri) ) {
							System.err.println("No changes since last commit.  Use -force-commit to commit anyway.");
							createCommit = false;
						}
					}
				}
				
				// If after those checks, we still want to create it, then go ahead and do so:
				if( createCommit ) {				
					RdfNode commit = importer.getCommitRdfNode(targetType, ref.targetUri, new Date(), author, message, parentCommitUris);
					String commitUri;
					commitUri = importer.saveHead(commit, name);
					if( verbosity > 0 ) {
						System.out.println( "Commit\t" + commitUri );
					}
					if( o instanceof File && commitUri != null ) {
						setParentCommitUri((File)o, commitUri);
					}
				}
			}
		}
	}
	
	public void runCheckoutCmd( String[] args ) {
		args = mergeConfiguredArgs("checkout", args);
		boolean exportFiles = true;
		boolean link = false;
		boolean merge = false;
		boolean replaceFiles = false;
		boolean keepFiles = false;
		String source = null;
		String dest = null;
		for( int i=0; i < args.length; ++i ) {
			String arg = args[i];
			if( arg.length() == 0 ) {
				System.err.println(CHECKOUT_USAGE);
				System.exit(1);
			} else if( "-merge".equals(arg) ) {
				merge = true;
			} else if( "-v".equals(arg) ) {
				Log.setLevel(Log.LEVEL_CHATTY);
			} else if( "-q".equals(arg) ) {
				Log.setLevel(Log.LEVEL_SILENT);
			} else if( "-link".equals(arg) ) {
				link = true;
			} else if( "-replace-existing".equals(arg) ) {
				replaceFiles = true;
				keepFiles = false;
			} else if( "-keep-existing".equals(arg) ) {
				replaceFiles = false;
				keepFiles = true;
			} else if( "-dirs-only".equals(arg) ) {
				exportFiles = false;
			} else if( "-h".equals(arg) || "-?".equals(arg) ) {
				System.out.println(CHECKOUT_USAGE);
				System.exit(0);
			} else if( arg.charAt(0) != '-' || "-".equals(arg) ) {
				if( source == null ) source = arg;
				else if( dest == null ) dest = arg;
				else {
					System.err.println("ccouch checkout: Too many arguments: " + arg);
					System.err.println(CHECKOUT_USAGE);
					System.exit(1);
				}
			} else {
				System.err.println("ccouch checkout: Unrecognised argument: " + arg);
				System.err.println(CHECKOUT_USAGE);
				System.exit(1);
			}
		}
		if( source == null ) {
			System.err.println("ccouch checkout: Source unspecified");
			System.err.println(CHECKOUT_USAGE);
			System.exit(1);
		}
		if( dest == null ) {
			System.err.println("ccouch checkout: Destination unspecified");
			System.err.println(CHECKOUT_USAGE);
			System.exit(1);
		}
		final Exporter exporter = new Exporter(getLocalGetter(), getRepository().getBlobIdentifier());
		exporter.link = link;
		exporter.exportFiles = exportFiles;
		exporter.replaceFiles = replaceFiles;
		if( keepFiles ) {
			exporter.mergeConflictHandler = new Exporter.MergeConflictHandler() {
				public boolean handleMergeConflict(String path, String localUrn,
						Object localObj, String remoteUrn, Object remoteObj) {
					Log.log( Log.LEVEL_WARNINGS, Log.TYPE_SKIP, path + "; " + localUrn + " != " + remoteUrn );
					return false;
				}
			};
		}
		File destFile = new File(dest);
		Object exportThis = exporter.followRedirects(new Ref(source), null);
		if( "-".equals(dest) ) {
			if( exportThis instanceof Blob ) {
				BlobUtil.writeBlobToOutputStream((Blob)exportThis, System.out);
				return;
			} else {
				Log.log(Log.LEVEL_ERRORS, Log.TYPE_ERROR, "Can't export " + exportThis.getClass().getName() + " to stdout");
				System.exit(1);
			}
		}
		if( destFile.exists() && destFile.isDirectory() && !merge ) {
			Log.log(Log.LEVEL_ERRORS, Log.TYPE_ERROR, "Destination '" + destFile + "' already exists.  Use -merge to merge directory trees.");
			System.exit(1);
		}
		if( destFile.exists() && !destFile.isDirectory() && !replaceFiles ) {
			Log.log(Log.LEVEL_ERRORS, Log.TYPE_ERROR, "Destination '" + destFile + "' already exists.  Use -replace-existing to replace existing files.");
			System.exit(1);
		}
		if( exportThis instanceof Commit ) {
			addParentCommitUri(destFile, ((Commit)exportThis).getUri());
		}
		exporter.exportObject(exportThis, destFile, null);
	}
	
	public boolean cache( String uri ) {
		Object o = getLocalGetter().get(uri);
		if( o == null ) {
			//reportCacheStatus(uri, verbosity, false);
			return false;
		} else if( o instanceof Directory ) {
			//reportCacheStatus(uri, verbosity, true);
			boolean success = true;
			Directory d = (Directory)o;
			for( Iterator i=d.getEntries().values().iterator(); i.hasNext(); ) {
				Directory.Entry e = (Directory.Entry)i.next();
				if( e.getTarget() instanceof Ref ) {
					if( !cache( ((Ref)e.getTarget()).targetUri ) ) success = false;
				}
			}
			return success;
		} else if( o instanceof Commit ) {
			//reportCacheStatus(uri, verbosity, true);
			Commit c = (Commit)o;
			o = c.getTarget();
		} else if( o instanceof RdfNode && RdfNamespace.CCOUCH_REDIRECT.equals(((RdfNode)o).typeName) ) {
			//reportCacheStatus(uri, verbosity, true);
			o = ((RdfNode)o).getSingle(RdfNamespace.CCOUCH_TARGET);
		}
		
		if( o instanceof Ref ) {
			return cache( ((Ref)o).targetUri );
		} else {
			return true;
		}
	}
	
	public void runCacheCmd( String[] args ) {
		args = mergeConfiguredArgs("cache", args);
		List cacheUris = new ArrayList();
		boolean cacheless = false;
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( "-q".equals(arg) ) {
				cacheVerbosity = 0;
			} else if( "-v".equals(arg) ) {
				cacheVerbosity = GetAttemptListener.GOT_FROM_LOCAL;
			} else if( "-?".equals(arg) || "-h".equals(arg) ) {
				System.out.println(CACHE_USAGE);
				System.exit(0);
			} else if( "-cacheless".equals(arg) ) {
				cacheless = true;
			} else if( !arg.startsWith("-") ) {
				cacheUris.add(arg);
			} else {
				System.err.println("ccouch cache: Unrecognised argument: " + arg);
				System.err.println(CACHE_USAGE);
				System.exit(1);
			}
		}
		if( getRepository().remoteCacheRepository == null && !cacheless ) {
			System.err.println("ccouch cache: The currently selected repository (" + getRepository().getPath() + ")");
			System.err.println("  has no cache repository set up.  'ccouch cache' is pretty pointless!");
			System.err.println("  Use -cacheless to run anyway");
			System.exit(1);
		}
		boolean success = true;
		for( Iterator i=cacheUris.iterator(); i.hasNext(); ) {
			if( !cache( (String)i.next() ) ) success = false;
		}
		System.exit(success ? 0 : 1);
	}

	protected static final String ALL_REMOTE_HEADS_PATH = "-all-remotes";
	
	protected boolean cacheHeads( ContentCouchRepository remote, String remotePath,
			ContentCouchRepository cache, String cachePath ) {
		Exporter e = new Exporter(getLocalGetter(), getRepository().getBlobIdentifier());
		Object ro = remote.getHead(remotePath);
		if( ro == null ) {
			return false;
		}
		e.exportObject(ro, new File(cache.getPath() + "heads/" + cachePath), "x-ccouch-head://" + remote.name + "/" + remotePath );
		return true;
	}
	
	protected boolean cacheHeads( String path ) {
		ContentCouchRepository cache = getRepository().remoteCacheRepository;
		
		if( path.startsWith(RdfNamespace.URI_PARSE_PREFIX)) path = path.substring(RdfNamespace.URI_PARSE_PREFIX.length());
		if( path.startsWith("x-ccouch-head:") ) path = path.substring("x-ccouch-head:".length());
		
		boolean success;
		if( ALL_REMOTE_HEADS_PATH.equals(path) ) {
			success = true;
			for( Iterator rri = getRepository().remoteRepositories.iterator(); rri.hasNext(); ) {
				ContentCouchRepository rr = (ContentCouchRepository)rri.next();
				if( rr.name != null ) {
					if( !cacheHeads( rr, rr.name + "/", cache, rr.name + "/" ) ) success = false;
				}
			}
		} else if( path.startsWith("//") ) {
			path = path.substring(2);
			int si = path.indexOf('/');
			if( si == -1 ) throw new RuntimeException("Malformed head path (contains '//' but no '/'): " + path);
			String repoName = path.substring(0,si);
			path = path.substring(si+1);
			ContentCouchRepository rr = (ContentCouchRepository)getRepository().namedRepositories.get(repoName);
			if( rr == null ) throw new RuntimeException("No such repository: " + repoName);
			if( path.endsWith("/latest") ) path = rr.findHead(path);
			success = cacheHeads( rr, path, cache, path );
		} else {
			success = false;

			if( path.startsWith("/") ) path = path.substring(1);
			
			String oPath = path;
			if( path.endsWith("/latest") ) path = getHeadGetter(true).findHead(path);
			if( path == null ) {
				Log.log( Log.LEVEL_WARNINGS, Log.TYPE_NOTFOUND, "Could not find latest head of " + oPath);
				return false;
			}
			
			for( Iterator rri = getRepository().remoteRepositories.iterator(); rri.hasNext(); ) {
				ContentCouchRepository rr = (ContentCouchRepository)rri.next();
				if( rr.name != null ) {
					if( cacheHeads( rr, path, cache, path) ) success = true;
				}
			}
		}
		if( !success ) {
			Log.log( Log.LEVEL_ERRORS, Log.TYPE_NOTFOUND, "x-ccouch-head:" + path);
		}

		return success;
	}
	
	public void runCacheHeadsCmd( String[] args ) {
		args = mergeConfiguredArgs("cache-heads", args);
		List cacheUris = new ArrayList();
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( "-q".equals(arg) ) {
				Log.setLevel(Log.LEVEL_SILENT);
			} else if( "-v".equals(arg) ) {
				Log.setLevel(Log.LEVEL_CHATTY);
			} else if( "-?".equals(arg) || "-h".equals(arg) ) {
				System.out.println(CACHE_HEADS_USAGE);
				System.exit(0);
			} else if( !arg.startsWith("-") || ALL_REMOTE_HEADS_PATH.equals(arg) ) {
				cacheUris.add(arg);
			} else {
				System.err.println("ccouch cache-heads: Unrecognised argument: " + arg);
				System.err.println(CACHE_HEADS_USAGE);
				System.exit(1);
			}
		}
		if( getRepository().remoteCacheRepository == null ) {
			System.err.println("ccouch cache: The currently selected repository (" + getRepository().getPath() + ")");
			System.err.println("  has no cache repository set up.  'ccouch cache-heads' is pretty pointless!");
			System.exit(1);
		}
		boolean success = true;
		for( Iterator i=cacheUris.iterator(); i.hasNext(); ) {
			if( !cacheHeads( (String)i.next() ) ) success = false;
		}
		System.exit(success ? 0 : 1);
	}

	public void runIdCmd( String[] args ) {
		args = mergeConfiguredArgs("id", args);
		runStoreCmd(concat(new String[]{"-dont-store"},args));
	}
	
	public void runCheckCmd( String[] args ) {
		args = mergeConfiguredArgs("check", args);
		List checkPaths = new ArrayList();
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( arg.startsWith("-" ) ) {
				System.err.println("ccouch check: Unrecognised argument: " + arg);
				System.err.println(CACHE_CHECK_USAGE);
				System.exit(1);
			} else {
				checkPaths.add(arg);
			}
		}
		
		if( checkPaths.size() == 0 ) {
			checkPaths.add(getRepository().getPath() + "data/");
		}
		
		RepoChecker rc = new RepoChecker();
		for( Iterator i=checkPaths.iterator(); i.hasNext(); ) {
			String path = (String)i.next();
			System.err.println("Checking " + path);
			File f = new File(path);
			rc.checkFiles(f);
		}
	}
	
	public void runRdfifyCmd( String[] args ) {
		args = mergeConfiguredArgs("rdfify", args);
		String dir = args[0];
		boolean nested = false;
		for( int i=0; i < args.length; ++i ) {
			String arg = args[i];
			if( arg.length() == 0 ) {
				System.err.println(RDFIFY_USAGE);
				System.exit(1);
			} else if( "-nested".equals(arg) ) {
				nested = true;
			} else if( arg.charAt(0) != '-' ) {
				dir = arg;
			} else {
				System.err.println("ccouch rdfify: Unrecognised argument: " + arg);
				System.err.println(RDFIFY_USAGE);
				System.exit(1);
			}
		}
		
		Importer imp = new Importer(getRepository());
		imp.shouldStoreFiles = false;
		imp.shouldStoreDirs = false;
		imp.shouldNestSubdirs = nested;
		
		if( dir == null ) {
			System.err.println("ccouch rdfify: No object specified" );
			System.exit(1);
		}

		Object o = getLocalGetter().get(dir);
		if( o == null ) {
			System.err.println("ccouch rdfify: Could not find " + dir);
			System.exit(1);
		}
		if( !(o instanceof Directory) ) {
			System.err.println("ccouch rdfify: Object specified is not a directory: " + dir);
			System.exit(1);
		}
		
		System.out.println(RdfIO.xmlEncodeRdf(imp.rdfifyDirectory(new FileDirectory(new File(dir))), RdfNamespace.CCOUCH_NS));
	}
	
	public void run( String[] args ) {
		if( args.length == 0 ) {
			System.err.println(USAGE);
			System.exit(1);
		}
		String cmd = null;
		int i;
		for( i=0; i<args.length; ) {
			int ni;
			if( "-h".equals(args[i]) || "-?".equals(args[i]) ) {
				System.out.println(USAGE);
				System.exit(0);
			} else if( (ni = getRepository().handleArguments(args, i)) > i ) {
				i = ni;
			} else if( args[i].length() > 0 && args[i].charAt(0) != '-' ) {
				cmd = args[i++];
				break;
			} else {
				System.err.println("ccouch: Unrecognised command: " + args[i]);
				System.err.println(USAGE);
				System.exit(1);
			}
		}
		if( cmd == null ) { 
			System.err.println("No command given");
			System.err.println(USAGE);
			System.exit(1);
		}
		String[] cmdArgs = new String[args.length-i];
		for( int j=0; j<cmdArgs.length; ++i, ++j ) {
			cmdArgs[j] = args[i];
		}
		if( "store".equals(cmd) ) {
			runStoreCmd( cmdArgs );
		} else if( "checkout".equals(cmd) ) {
			runCheckoutCmd( cmdArgs );
		} else if( "cache".equals(cmd) ) {
			runCacheCmd( cmdArgs );
		} else if( "cache-heads".equals(cmd) ) {
			runCacheHeadsCmd( cmdArgs );
		} else if( "check".equals(cmd) ) {
			runCheckCmd( cmdArgs );
		} else if( "id".equals(cmd) ) {
			runIdCmd( cmdArgs );
		} else if( "rdfify".equals(cmd) ) {
			runRdfifyCmd( cmdArgs );
		} else {
			System.err.println("ccouch: Unrecognised sub-command: " + cmd);
			System.err.println(USAGE);
			System.exit(1);
		}
	}
	
	public static void main( String[] args ) {
		new ContentCouchCommand().run( args );
	}
}
