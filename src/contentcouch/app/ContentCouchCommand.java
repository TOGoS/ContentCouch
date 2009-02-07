// -*- tab-width:4 -*-
package contentcouch.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import contentcouch.file.FileUtil;
import contentcouch.hashcache.FileHashCache;
import contentcouch.hashcache.SimpleListFile;
import contentcouch.store.BlobStore;
import contentcouch.store.FileBlobMap;
import contentcouch.store.Getter;
import contentcouch.store.MultiGetter;
import contentcouch.store.NoopPutter;
import contentcouch.store.Putter;
import contentcouch.store.Sha1BlobStore;
import contentcouch.xml.RDF;

public class ContentCouchCommand {
	public static String OPTION_REPO_PATH = "repo-path";
	public static String OPTION_DONT_STORE = "dont-store";
	
	public String USAGE =
		"Usage: ccouch [general options] <sub-command> [command-args]\n" +
		"Run ccouch <subcommand> -? for further help\n" +
		"General options:\n" +
		"  -repo <path>   ; specify a local repository to use\n" +
		"Sub-commands:\n" +
		"  store <files>         ; store files in the repo\n" +
		"  checkout <uri> <dest> ; check files out to the filesystem\n" +
		"  id <files>            ; give URNs for files without storing\n" +
		"  check                 ; check repo integrity and delete bad files";
	
	public String STORE_USAGE =
		"Usage: ccouch [general options] store [store options] <file1> <file2> ...\n" +
		"Store options:\n" +
		"  -m <message>       ; create a commit with this message\n" +
		"  -a <author>        ; create a commit with this author\n" +
		"  -n <name>          ; name your commit this\n" +
		"  -link              ; hardlink files into the store instead of copying\n" +
		"  -files-only        ; store only file content (no directory listings)\n" +
		"  -relink            ; hardlink imported files to their stored counterpart\n" +
		"  -v                 ; verbose - report every path -> urn mapping\n" +
		"  -q                 ; quiet - show nothing\n" +
		"  -?                 ; display help and exit\n" +
		"\n" +
		"If -m, -a, and/or -n are used, a commit will be created and its URN output.\n" +
		"\n" +
		"If -n is specified, a commit will be stored under that name as\n" +
		"<repo-path>/heads/local/<name>/<version>, where <version> is automatically\n" +
		"incremented for new commits.\n" +
		"\n" +
		"-relink is useful when a copy of the file is already in the\n" +
		"repository and you want to make sure the data ends up being\n" +
		"shared.  -relink implies -link.";
	
	public String CHECKOUT_USAGE =
		"Usage: ccouch [general options] checkout [checkout options] <source> <dest>\n" +
		"Checkout options:\n" +
		"  -link              ; hardlink files from the store instead of copying\n" +
		"  -v                 ; verbose - report every file exported\n" +
		"  -?                 ; display help and exit";

	protected String repoPath = ".";
	protected ContentCouchRepository repositoryCache = null;
	
	public ContentCouchRepository getRepository() {
		if( repositoryCache == null ) {
			repositoryCache = ContentCouchRepository.getIfExists(repoPath);
		}
		return repositoryCache;
	}
	
	public BlobStore getDatastore( Map options ) {
		FileBlobMap fbm;
		Putter putter;
		String repoPath = (String)options.get(OPTION_REPO_PATH);
		if( Boolean.TRUE.equals(options.get(OPTION_DONT_STORE)) ) {
			putter = new NoopPutter();
			fbm = null;
		} else {
			if( repoPath == null ) return null;
			fbm = new FileBlobMap(repoPath + "/data/");
			putter = fbm;
		}
		Sha1BlobStore bs = new Sha1BlobStore(fbm, putter);
		SimpleListFile slf;
		if( repoPath != null ) {
			File cf = new File(repoPath + "/cache/file-attrs.slf");
			try {
				FileUtil.mkParentDirs(cf);
				slf = new SimpleListFile(cf, "rw");
				slf.init(65536, 1024*1024);
			} catch( IOException e ) {
				try {
					System.err.println("Couldn't open " + cf + " in 'rw' mode, trying 'r'");
					slf = new SimpleListFile(cf, "r");
					slf.init(65536, 1024*1024);
				} catch( IOException ee ) {
					throw new RuntimeException(ee);
				}
			}
			bs.fileHashCache = new FileHashCache(slf);
		}
		return bs;
	}
	
	public Getter getBlobGetter( Map options ) {
		MultiGetter mbg = new MultiGetter();
		mbg.addGetter(getDatastore(options));
		mbg.addGetter(new FileBlobMap(""));
		return mbg;
	}
	
	public FileBlobMap getNamedStore( Map options ) {
		String repoPath = (String)options.get(OPTION_REPO_PATH);
		if( repoPath == null ) {
			return null;
		} else {
			return new FileBlobMap(repoPath + "/heads/");
		}
	}
	
	public Importer getImporter( Map options ) {
		BlobStore ds = getDatastore(options);
		if( ds == null ) throw new RuntimeException("Datastore unspecified");
		FileBlobMap namedStore = getNamedStore(options);
		return new Importer(ds, namedStore);
	}
	
	public void runStoreCmd( String[] args, Map options ) {
		List files = new ArrayList();
		String message = null;
		String name = null;
		String author = null;
		final Importer importer = getImporter(options);
		int verbosity = 1;
		boolean importFilesOnly = false;
		for( int i=0; i < args.length; ++i ) {
			String arg = args[i];
			if( arg.length() == 0 ) {
				System.err.println(STORE_USAGE);
				System.exit(1);
			} else if( "-v0".equals(arg) ) {
				verbosity = 0;
			} else if( "-v".equals(arg) ) {
				verbosity = 2;
			} else if( "-files-only".equals(arg) ) {
				importFilesOnly = true;
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
		if( files.size() == 0 ) {
			System.err.println("ccouch store: No files given");
			System.err.println(STORE_USAGE);
			System.exit(1);
		}
		final boolean createCommit;
		if( name != null || message != null || author != null ) {
			createCommit = true;
			if( files.size() != 1 ) {
				System.err.println("Cannot use -m or -n with more than one file");
				System.exit(1);
			}
		} else {
			createCommit = false;
		}

		final boolean showAllMappings = (
			(importFilesOnly && verbosity >= 1) ||
			(verbosity >= 2)
		);
		
		FileImportListener fileImportListener = new FileImportListener() {
			public void fileImported(File file, String urn) {
				if( showAllMappings ) {
					System.out.println(importer.getFileUri(file) + "\t" + urn);
				}
			}
		};
		
		for( Iterator i=files.iterator(); i.hasNext(); ) {
			File file = new File((String)i.next());
			if( importFilesOnly ) {
				importer.recursivelyImportFiles( file, fileImportListener );
			} else {
				String urn = importer.importFileOrDirectory( file, fileImportListener );
				if( !showAllMappings && verbosity > 0 ) {
					// otherwise, this will already have been printed by our listener
					System.out.println(importer.getFileUri(file) + "\t" + urn);
				}
				if( createCommit ) {
					String targetType;
					if( urn.charAt(0) == '@' ) {
						targetType = RDF.OBJECT_TYPE_DIRECTORY;
					} else {
						targetType = RDF.OBJECT_TYPE_BLOB;
					}
					if( name != null ) name = "local/" + name;
					String commitUri = importer.saveHead(name, targetType, urn, new Date(), author, message, null);
					if( verbosity > 0 ) {
						System.out.println( "Committed " + commitUri );
					}
				}
			}
		}
	}
	
	public void runCheckoutCmd( String[] args, Map options ) {
		boolean verbose = false;
		boolean link = false;
		String source = null;
		String dest = null;
		for( int i=0; i < args.length; ++i ) {
			String arg = args[i];
			if( arg.length() == 0 ) {
				System.err.println(CHECKOUT_USAGE);
				System.exit(1);
			} else if( "-v".equals(arg) ) {
				verbose = true;
			} else if( "-link".equals(arg) ) {
				link = true;
			} else if( "-h".equals(arg) || "-?".equals(arg) ) {
				System.out.println(CHECKOUT_USAGE);
				System.exit(0);
			} else if( arg.charAt(0) != '-' ) {
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
		final Exporter exporter = new Exporter(getBlobGetter(options));
		exporter.link = link;
		exporter.verbose = verbose;
		File destFile = new File(dest);
		exporter.exportObject(source, destFile);
	}
	
	public void runIdCmd( String[] args, Map options ) {
		options = new HashMap(options);
		options.put(OPTION_DONT_STORE, Boolean.TRUE);
		runStoreCmd(args, options);
	}
	
	public void runCheckCmd( String[] args, Map options ) {
		String rp = (String)options.get(OPTION_REPO_PATH);
		if( rp == null ) {
			System.err.println("Repository unspecified");
		}
		RepoChecker rc = new RepoChecker();
		rc.checkFiles(new File(rp + "/data"));
	}
	
	public void run( String[] args ) {
		if( args.length == 0 ) {
			System.err.println(USAGE);
			System.exit(1);
		}
		String cmd = null;
		Map options = new HashMap();
		int i;
		for( i=0; i<args.length; ++i ) {
			if( "-h".equals(args[i]) || "-?".equals(args[i]) ) {
				System.out.println(USAGE);
				System.exit(0);
			} else if( "-repo".equals(args[i]) ) {
				options.put(OPTION_REPO_PATH, args[++i]);
			} else if( args[i].length() > 0 && args[i].charAt(0) != '-' ) {
				cmd = args[i++];
				break;
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
			runStoreCmd( cmdArgs, options );
		} else if( "checkout".equals(cmd) ) {
			runCheckoutCmd( cmdArgs, options );
		} else if( "check".equals(cmd) ) {
			runCheckCmd( cmdArgs, options );
		} else if( "id".equals(cmd) ) {
			runIdCmd( cmdArgs, options );
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

// Used: 42294853632
// Copied yayshots
// Used: 42331856896

// Stored yayshots-relink-test without relinking
// Used: 42338385920
// Used: 42341187584

// After apparently successful relink of files in yayshots-relink-test
// Used: 52309570560
