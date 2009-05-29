// -*- tab-width:4 -*-
package contentcouch.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import contentcouch.blob.BlobUtil;
import contentcouch.repository.MetaRepoConfig;
import contentcouch.store.TheGetter;
import contentcouch.value.Blob;

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
		"  -sector            ; data/sub-directory to store data (defaults to \"user\")\n" +
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
		"  -v       ; show all URNs being followed\n" +
		"  -q       ; show nothing - not even failures\n" +
		"  -sector  ; data/sub-directory to store data (defaults to \"remote\")\n" +
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
	
	protected MetaRepoConfig metaRepoConfig = new MetaRepoConfig();
	
	protected String[] concat( String[] s1, String[] s2 ) {
		String[] r = new String[s1.length+s2.length];
		int j = 0;
		for( int i=0; i<s1.length; ++i, ++j ) r[j] = s1[i];
		for( int i=0; i<s2.length; ++i, ++j ) r[j] = s2[i];
		return r;
	}
	
	protected String[] mergeConfiguredArgs( String commandName, String[] commandLineArgs ) {
		return concat( metaRepoConfig.getCommandArgs(commandName), commandLineArgs );
	}
	
	//// Dump stuff ////
	
	protected void dumpRepoConfig( MetaRepoConfig repo, PrintStream ps, String pfx ) {
		// TODO
		/*
		ps.println( pfx + "Repository path: " + repo.getPath() );
		ps.println( pfx );
		
		if( repo.namedRepositories != null && repo.namedRepositories.size() > 0 ) {
			ps.println( pfx + "Named repositories:" );
			for( Iterator i = repo.namedRepositories.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry e = (Map.Entry)i.next();
				ps.println( pfx + "  " + e.getKey() + ": " + ((MetaRepository)e.getValue()).getPath() );
			}
		}
		ps.println( pfx );
		
		if( repo.localRepositories != null && repo.localRepositories.size() > 0 ) {
			ps.println( pfx + "Local repositories:" );
			for( Iterator i = repo.localRepositories.iterator(); i.hasNext(); ) {
				ps.println( pfx + "  " + ((MetaRepository)i.next()).getPath() );
			}
		}

		if( repo.remoteRepositories != null && repo.remoteRepositories.size() > 0 ) {
			ps.println( pfx + "Remote repositories:" );
			for( Iterator i = repo.remoteRepositories.iterator(); i.hasNext(); ) {
				ps.println( pfx + "  " + ((MetaRepository)i.next()).getPath() );
			}
		}

		ps.println( pfx + "Sub-command default arguments:" );
		for( Iterator i=repo.cmdArgs.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			ps.println( pfx + "  [" + e.getKey() + "]" );
			List argList = (List)e.getValue();
			for( Iterator argListI=argList.iterator(); argListI.hasNext(); ) {
				ps.println( pfx + "    " + argListI.next() );
			}
		}
		*/
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
		int verbosity = 1;
		boolean storeFiles = true;
		boolean storeDirs = true;
		boolean storeCommits = true;
		boolean forceCommit = false;
		boolean dumpConfig = false;
		boolean shouldLinkStored = false;
		boolean shouldRelinkImported = false;
		String storeSector = "user";
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
				shouldLinkStored = true;
			} else if( "-relink".equals(arg) ) {
				shouldLinkStored = true;
				shouldRelinkImported = true;
			} else if( "-sector".equals(arg) ) {
				storeSector = args[++i];
			} else if( "-m".equals(arg) ) {
				message = args[++i];
			} else if( "-n".equals(arg) ) {
				name = args[++i];
			} else if( "-a".equals(arg) ) {
				author = args[++i];
			} else if( "-force-commit".equals(arg) ) {
				forceCommit = true;
			} else if( "-dump-config".equals(arg) ) {
				dumpConfig = true;
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
		
		// TODO:
		
		System.err.println("store unimplemented!");
		System.exit(1);
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
		
		if( "-".equals(dest) ) {
			Object value = TheGetter.get(source);
			if( value == null ) {
				System.err.println("Could not find " + source);
				System.exit(1);
			} else if( value instanceof Blob ) {
				BlobUtil.writeBlobToOutputStream((Blob)value, System.out);
				System.exit(0);
			} else {
				System.out.println(value.toString());
				System.exit(0);
			}
		}
		
		// TODO
		
		System.err.println("checkout unimplemented!");
		System.exit(1);
	}
	
	public void runCacheCmd( String[] args ) {
		args = mergeConfiguredArgs("cache", args);
		List cacheUris = new ArrayList();
		String storeSector = "remote";
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( "-q".equals(arg) ) {
			} else if( "-v".equals(arg) ) {
			} else if( "-sector".equals(arg) ) {
				storeSector = args[++i];
			} else if( "-?".equals(arg) || "-h".equals(arg) ) {
				System.out.println(CACHE_USAGE);
				System.exit(0);
			} else if( !arg.startsWith("-") ) {
				cacheUris.add(arg);
			} else {
				System.err.println("ccouch cache: Unrecognised argument: " + arg);
				System.err.println(CACHE_USAGE);
				System.exit(1);
			}
		}
		
		// TODO
		
		System.err.println("cache unimplemented!");
		System.exit(1);
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
			} else if( !arg.startsWith("-") ) {
				cacheUris.add(arg);
			} else {
				System.err.println("ccouch cache-heads: Unrecognised argument: " + arg);
				System.err.println(CACHE_HEADS_USAGE);
				System.exit(1);
			}
		}
		// TODO: implement
		System.err.println("cache-heads unimplemented!");
		System.exit(1);
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
			checkPaths.add(metaRepoConfig.defaultRepoConfig.uri + "data/");
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
		String storeSector = "rdfify"; // This shouldn't actually show up...
		for( int i=0; i < args.length; ++i ) {
			String arg = args[i];
			if( arg.length() == 0 ) {
				System.err.println(RDFIFY_USAGE);
				System.exit(1);
			} else if( "-sector".equals(arg) ) {
				storeSector = args[++i];
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
		
		System.err.println("rdfify unimplemented!");
		System.exit(1);
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
			} else if( (ni = metaRepoConfig.handleArguments(args, i, "./")) > i ) {
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
		
		TheGetter.globalInstance = metaRepoConfig.getRequestKernel();
		
		if( "config".equals(cmd) ) {
			System.out.println("Repo configuration:");
			dumpRepoConfig( metaRepoConfig, System.out, "  ");
		} else if( "store".equals(cmd) ) {
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
