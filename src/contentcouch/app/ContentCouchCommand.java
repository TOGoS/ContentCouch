// -*- tab-width:4 -*-
package contentcouch.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import contentcouch.file.FileDirectory;
import contentcouch.file.FileUtil;
import contentcouch.rdf.RdfDirectory;
import contentcouch.rdf.RdfIO;
import contentcouch.rdf.RdfNamespace;
import contentcouch.rdf.RdfNode;
import contentcouch.store.FileBlobMap;
import contentcouch.store.Getter;
import contentcouch.store.MultiGetter;
import contentcouch.value.Ref;

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
		"  rdfify <dir>          ; print RDF listing of a directory\n" +
		"  check                 ; check repo integrity and delete bad files";
	
	public String STORE_USAGE =
		"Usage: ccouch [general options] store [store options] <file1> <file2> ...\n" +
		"Store options:\n" +
		"  -m <message>       ; create a commit with this message\n" +
		"  -a <author>        ; create a commit with this author\n" +
		"  -n <name>          ; name your commit this\n" +
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
		"  -dirs-only         ; only export the directory structure\n" +
		"  -v                 ; verbose - report every file exported\n" +
		"  -?                 ; display help and exit";

	////
	
	protected String repoPath = ".";
	protected ContentCouchRepository repositoryCache = null;
	
	public ContentCouchRepository getRepository() {
		if( repositoryCache == null ) {
			repositoryCache = new ContentCouchRepository(repoPath);
		}
		return repositoryCache;
	}
		
	public Importer getImporter() {
		return new Importer(getRepository());
	}
	
	public Getter getLocalGetter() {
		MultiGetter mg = new MultiGetter();
		mg.addGetter(getRepository());
		mg.addGetter(new FileBlobMap(""));
		return mg;
	}
	
	public void runStoreCmd( String[] args ) {
		List files = new ArrayList();
		String message = null;
		String name = null;
		String author = null;
		final Importer importer = getImporter();
		int verbosity = 1;
		boolean storeFiles = true;
		boolean storeDirs = true;
		boolean storeCommits = true;
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
					uri = null;
				}
				if( uri != null ) System.out.println(uri + "\t" + urn);
			}
		};
		
		importer.shouldStoreFiles = storeFiles;
		importer.shouldStoreDirs = storeDirs;
		importer.shouldStoreHeads = storeCommits;
		
		for( Iterator i=files.iterator(); i.hasNext(); ) {
			File file = new File((String)i.next());
			Ref ref = importer.importObject(FileUtil.getContentCouchObject(file));
			
			boolean showFinal;
			if( file.isDirectory() ) {
				showFinal = !showIntermediateDirs;
			} else {
				showFinal = !showIntermediateFiles;
			}
			
			if( showFinal && verbosity > 0 ) {
				System.out.println(importer.getFileUri(file) + "\t" + ref.targetUri);
			}
			if( createCommit ) {
				String targetType;
				if( ref.targetUri.charAt(0) == '@' || ref.targetUri.startsWith("x-parse-rdf:") ) {
					targetType = RdfNamespace.OBJECT_TYPE_DIRECTORY;
				} else {
					targetType = RdfNamespace.OBJECT_TYPE_BLOB;
				}
				if( name != null ) name = "local/" + name;
				
				RdfNode commit = importer.getCommitRdfNode(targetType, ref.targetUri, new Date(), author, message, null);
				String commitUri;
				commitUri = importer.saveHead(commit, name);
				if( verbosity > 0 ) {
					System.out.println( "Commit\t" + commitUri );
				}
			}
		}
	}
	
	public void runCheckoutCmd( String[] args ) {
		boolean verbose = false;
		boolean exportFiles = true;
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
			} else if( "-dirs-only".equals(arg) ) {
				exportFiles = false;
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
		final Exporter exporter = new Exporter(getLocalGetter());
		exporter.link = link;
		exporter.verbose = verbose;
		exporter.exportFiles = exportFiles;
		File destFile = new File(dest);
		exporter.exportObject(new Ref(source), destFile, null);
	}
	
	protected String[] concat( String[] s1, String[] s2 ) {
		String[] r = new String[s1.length+s2.length];
		int j = 0;
		for( int i=0; i<s1.length; ++i, ++j ) r[j] = s1[i];
		for( int i=0; i<s2.length; ++i, ++j ) r[j] = s2[i];
		return r;
	}
	
	public void runIdCmd( String[] args ) {
		runStoreCmd(concat(new String[]{"-dont-store"},args));
	}
	
	public void runCheckCmd( String[] args ) {
		RepoChecker rc = new RepoChecker();
		rc.checkFiles(new File(repoPath + "/data"));
	}
	
	public void runRdfifyCmd( String[] args ) {
		String dir = args[0];
		System.out.println(RdfIO.xmlEncodeRdf(new RdfDirectory(new FileDirectory(new File(dir))), RdfNamespace.CCOUCH_NS));
	}
	
	public void run( String[] args ) {
		if( args.length == 0 ) {
			System.err.println(USAGE);
			System.exit(1);
		}
		String cmd = null;
		int i;
		for( i=0; i<args.length; ++i ) {
			if( "-h".equals(args[i]) || "-?".equals(args[i]) ) {
				System.out.println(USAGE);
				System.exit(0);
			} else if( "-repo".equals(args[i]) ) {
				repoPath = args[++i];
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
			runStoreCmd( cmdArgs );
		} else if( "checkout".equals(cmd) ) {
			runCheckoutCmd( cmdArgs );
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

// Used: 42294853632
// Copied yayshots
// Used: 42331856896

// Stored yayshots-relink-test without relinking
// Used: 42338385920
// Used: 42341187584

// After apparently successful relink of files in yayshots-relink-test
// Used: 52309570560
