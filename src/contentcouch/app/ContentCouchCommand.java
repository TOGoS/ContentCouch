// -*- tab-width:4 -*-
package contentcouch.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import contentcouch.store.BlobGetter;
import contentcouch.store.BlobPutter;
import contentcouch.store.BlobStore;
import contentcouch.store.FileBlobMap;
import contentcouch.store.MultiBlobGetter;
import contentcouch.store.NoopBlobPutter;
import contentcouch.store.Sha1BlobStore;
import contentcouch.xml.RDF;

public class ContentCouchCommand {
	
	public String USAGE =
		"Usage: ccouch [general options] <sub-command> [command-args]\n" +
		"Run ccouch <subcommand> -? for further help\n" +
		"General options:\n" +
		"  -repo <path>   ; specify a local repository to use\n" +
		"Sub-commands:\n" +
		"  store <files>         ; store files in the repo\n" +
		"  checkout <uri> <dest> ; check files out to the filesystem\n" +
		"  id <files>            ; give URNs for files without storing";
	
	public String STORE_USAGE =
		"Usage: ccouch [general options] store [store options] <file1> <file2> ...\n" +
		"Store options:\n" +
		"  -m <message>       ; create a commit with this message\n" +
		"  -a <author>        ; create a commit with this author\n" +
		"  -n <name>          ; name your commit this\n" +
		"  -link              ; hardlink files into the store instead of copying\n" +
		"  -relink            ; hardlink imported files to their stored counterpart\n" +
		"  -v                 ; verbose - report every path -> urn mapping\n" +
		"\n" +
		"If -m, -a, and/or -n are used, a commit will be created and its URN output.\n" +
		"\n" +
		"If -n is specified, a commit will be stored under that name as\n" +
		"<repo-path>/heads/local/<name>/<version>, where <version> is automatically\n" +
		"incremented for new commits.";

	protected String repoPath = ".";
	protected ContentCouchRepository repositoryCache = null;
	
	public ContentCouchRepository getRepository() {
		if( repositoryCache == null ) {
			repositoryCache = ContentCouchRepository.getIfExists(repoPath);
		}
		return repositoryCache;
	}
	
	public BlobStore getDatastore( Map options ) {
		String repoPath = (String)options.get("repo-path");
		if( repoPath == null ) {
			return null;
		}
		FileBlobMap fbm = new FileBlobMap(repoPath + "/data/");
		BlobPutter putter;
		if( Boolean.TRUE.equals(options.get("NoStore")) ) {
			putter = new NoopBlobPutter();
		} else {
			putter = fbm;
		}
		return new Sha1BlobStore(fbm, putter);
	}
	
	public BlobGetter getBlobGetter( Map options ) {
		MultiBlobGetter mbg = new MultiBlobGetter();
		mbg.addBlobGetter(getDatastore(options));
		mbg.addBlobGetter(new FileBlobMap(""));
		return mbg;
	}
	
	public FileBlobMap getNamedStore( Map options ) {
		String repoPath = (String)options.get("repo-path");
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
		if( namedStore == null ) throw new RuntimeException("Named store unspecified");
		return new Importer(ds, namedStore);
	}
	
	public void runStoreCmd( String[] args, Map options ) {
		List files = new ArrayList();
		String message = null;
		String name = null;
		String author = null;
		final Importer importer = getImporter(options);
		boolean pShowSubMappings = false;
		for( int i=0; i < args.length; ++i ) {
			String arg = args[i];
			if( arg.length() == 0 ) {
				System.err.println(STORE_USAGE);
				System.exit(1);
			} else if( "-v".equals(arg) ) {
				pShowSubMappings = true;
			} else if( "-link".equals(arg) ) {
				importer.shouldLinkStored = true;
			} else if( "-relink".equals(arg) ) {
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

		final boolean showSubMappings = pShowSubMappings;
		
		FileImportListener fileImportListener = new FileImportListener() {
			public void fileImported(File file, String urn) {
				if( showSubMappings ) {
					System.out.println(importer.getFileUri(file) + "\t" + urn);
				}
			}
		};
		
		for( Iterator i=files.iterator(); i.hasNext(); ) {
			File file = new File((String)i.next());
			String urn = importer.importFileOrDirectory( file, fileImportListener );
			if( !showSubMappings ) {
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
				System.out.println( "Committed " + commitUri );
			}
		}
	}
	
	public void runCheckoutCmd( String[] args, Map options ) {
		final Exporter exporter = new Exporter(getBlobGetter(options));
		String urn = args[0];
		String dest = args[1];
		File destFile = new File(dest);
		exporter.exportObject(urn, destFile);
	}
	
	public void runIdCmd( String[] args, Map options ) {
		options = new HashMap(options);
		options.put("NoStore", Boolean.TRUE);
		runStoreCmd(args, options);
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
				options.put("repo-path", args[++i]);
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