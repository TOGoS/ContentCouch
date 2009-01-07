// -*- tab-width:4 -*-
package contentcouch.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import contentcouch.store.BlobStore;
import contentcouch.store.Sha1BlobStore;

public class ContentCouchCommand {
	
	public String USAGE =
		"Usage: ccouch [general options] <sub-command> [command-args]\n" +
		"Run ccouch <subcommand> -? for further help\n" +
		"General options:\n" +
		"  -repo <path>   ; specify a local repository to use\n" +
		"Sub-commands:\n" +
		"  store <file>\n" +
		"  checkout <uri> <dest>";
	
	public String STORE_USAGE =
		"Usage: ccouch [general options] store [store options] <file1> <file2> ...\n" +
		"Store options:\n" +
		"  -link              ; hardlink files into the store instead of copying\n" +
		"  -relink            ; hardlink imported files to their stored counterpart\n" +
		"  -show-sub-mappings ; report every path -> urn mapping";

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
		} else {
			return new Sha1BlobStore(repoPath + "/data/");
		}
	}
	
	public Importer getImporter( Map options ) {
		BlobStore ds = getDatastore(options);
		if( ds == null ) throw new RuntimeException("Repository unspecified");
		return new Importer(ds);
	}
	
	public void runStoreCmd( String[] args, Map options ) {
		List files = new ArrayList();
		final Importer importer = getImporter(options);
		boolean pShowSubMappings = false;
		for( int i=0; i < args.length; ++i ) {
			String arg = args[i];
			if( arg.length() == 0 ) {
				System.err.println(STORE_USAGE);
				System.exit(1);
			} else if( "-show-sub-mappings".equals(arg) ) {
				pShowSubMappings = true;
			} else if( "-link".equals(arg) ) {
				importer.shouldLinkStored = true;
			} else if( "-relink".equals(arg) ) {
				importer.shouldRelinkImported = true;
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
			System.exit(0);
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
		}
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