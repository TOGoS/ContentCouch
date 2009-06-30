// -*- tab-width:4 -*-
package contentcouch.app;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import togos.rra.BaseRequest;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.blob.BlobUtil;
import contentcouch.directory.DirectoryWalker;
import contentcouch.directory.EntryFilters;
import contentcouch.directory.FilterIterator;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.SimpleCommit;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.rdf.RdfCommit;
import contentcouch.rdf.RdfDirectory;
import contentcouch.repository.MetaRepoConfig;
import contentcouch.repository.RepoConfig;
import contentcouch.repository.MetaRepository.RepoRef;
import contentcouch.store.TheGetter;
import contentcouch.stream.InternalStreamRequestHandler;
import contentcouch.value.BaseRef;
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
	
	public String COPY_USAGE =
		"Usage: ccouch [general options] copy [copy options] <src> <src> ... <dest>\n" +
		"<src> and <dest> can be file paths, URIs, or \"-\"\n";

	public String ID_USAGE =
		"Usage: ccouch [general options] id <uri> <uri> ...\n" +
		"Options:\n" +
		"  -hide-inputs  ; do not show input URIs";
	
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
		"  -store-sector      ; data sub-dir to store data (defaults to \"user\")\n" +
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
		"  -v            ; show all URNs being followed\n" +
		"  -q            ; show nothing - not even failures\n" +
		"  -link         ; hardlink files from the store instead of copying\n" +
		"  -store-sector <name> ; data subdir to store data (defaults to \"remote\")\n" +
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
	
	protected void printRepoConfig( RepoConfig repoConfig, PrintStream ps, String pfx ) {
		ps.println(pfx + repoConfig.disposition + " repository " + repoConfig.name );
		ps.println(pfx + "  " + "URI: " + repoConfig.uri );
	}
	
	protected int dumpRepoConfig( MetaRepoConfig mrc, PrintStream ps, String pfx ) {
		for( Iterator i=mrc.loadedFromConfigUris.iterator(); i.hasNext(); ) {
			ps.println(pfx + "Loaded from " + i.next());
		}
		List allRepoConfigs = mrc.getAllRepoConfigs();
		for( Iterator i=allRepoConfigs.iterator(); i.hasNext(); ) {
			RepoConfig repoConfig = (RepoConfig)i.next();
			ps.println(pfx);
			printRepoConfig(repoConfig, ps, pfx);
		}
		return 0;
	}
	
	//// Commit tracking ////
	
	protected String getCommitListUri(String checkoutUri) {
		if( checkoutUri.endsWith("/") ) {
			return PathUtil.appendPath(checkoutUri, ".commit-uris");
		}
		return null;
	}
	
	protected Response writeCommitUris( String commitListUri, Collection commitUris ) {
		String text = "";
		for( Iterator i=commitUris.iterator(); i.hasNext(); ) {
			String uri = (String)i.next();
			text += uri;
			text += "\n";
		}
		
		BaseRequest storeCommitUriReq = new BaseRequest(Request.VERB_PUT, commitListUri);
		storeCommitUriReq.putMetadata(CcouchNamespace.REQ_FILEMERGE_METHOD, CcouchNamespace.REQ_FILEMERGE_REPLACE);
		storeCommitUriReq.content = text;
		return TheGetter.handleRequest(storeCommitUriReq);
	}
	
	protected Response writeCommitUri( String commitListUri, String commitUri ) {
		TreeSet commitUris = new TreeSet();
		commitUris.add(commitUri);
		return writeCommitUris( commitListUri, commitUris );
	}
	
	protected List getCommitUris( String commitListUri ) {
		// Load parent commits
		BaseRequest parentCommitListReq = new BaseRequest(Request.VERB_GET, commitListUri);
		Response parentCommitListRes = TheGetter.handleRequest(parentCommitListReq);
		List commitUris = new ArrayList();
		if( parentCommitListRes.getStatus() == Response.STATUS_NORMAL ) {
			String parentCommitStr = ValueUtil.getString( parentCommitListRes.getContent() );
			String[] lines = parentCommitStr.split("\\s+");
			for( int lineNum=0; lineNum<lines.length; ++lineNum ) {
				String line = lines[lineNum];
				if( line.startsWith("#") ) continue;
				commitUris.add(line);
			}
		}
		return commitUris;
	}
	
	protected Response addCommitUri( String commitListUri, String commitUri ) {
		TreeSet commitUris = new TreeSet(getCommitUris( commitListUri ));
		commitUris.add(commitUri);
		return writeCommitUris( commitListUri, commitUris );
	}
	
	protected String normalizeUri( String uriOrPathOrSomething, boolean output, boolean directory ) {
		if( "-".equals(uriOrPathOrSomething) ) {
			return output ? "x-internal-stream:stdout" : "x-internal-stream:stdin";
		}
		if( directory && uriOrPathOrSomething.matches("^https?://.*/$") ) {
			return "active:contentcouch.directoryize+operand@" + UriUtil.uriEncode(uriOrPathOrSomething);
		}
		if( PathUtil.isUri(uriOrPathOrSomething) ) {
			return uriOrPathOrSomething;
		}
		if( new File(uriOrPathOrSomething).isDirectory() ) {
			if( uriOrPathOrSomething.endsWith("/.") ) {
				uriOrPathOrSomething = uriOrPathOrSomething.substring(0, uriOrPathOrSomething.length()-1);
			} else if( uriOrPathOrSomething.endsWith("/") ) {
			} else {
				uriOrPathOrSomething += "/";				
			}
		}
		return "file:" + uriOrPathOrSomething;
	}
	
	protected static class GeneralOptions {
		public boolean showHelp;
		public boolean shouldLinkStored;
		public boolean shouldRelinkImported;
		public boolean shouldDumpConfig;
		public boolean shouldSaveCommitUri; 
		public boolean shouldUseCommitTargets = false;
		public String storeSector;
		public String fileMergeMethod = CcouchNamespace.REQ_FILEMERGE_STRICTIG;
		public String dirMergeMethod = null;
		public List uris = new ArrayList();
	}
	
	protected GeneralOptions getGeneralOptions( String[] args, String commandName ) {
		args = mergeConfiguredArgs(commandName, args);
		
		GeneralOptions opts = new GeneralOptions();
		for( int i=0; i < args.length; ++i ) {
			String arg = args[i];
			if( arg.length() == 0 ) {
				return null;
			
			// Linking options
			} else if( "-link".equals(arg) ) {
				opts.shouldLinkStored = true;
			} else if( "-relink".equals(arg) ) {
				opts.shouldLinkStored = true;
				opts.shouldRelinkImported = true;

			// Merging options:
			} else if( "-file-merge-method".equals(arg) ) {
				opts.fileMergeMethod = args[++i];
			} else if( "-dir-merge-method".equals(arg) ) {
				opts.dirMergeMethod = args[++i];
			} else if( "-replace-existing".equals(arg) ) {
				opts.fileMergeMethod = CcouchNamespace.REQ_FILEMERGE_REPLACE;
			} else if( "-keep-existing".equals(arg) ) {
				opts.fileMergeMethod = CcouchNamespace.REQ_FILEMERGE_IGNORE;
			} else if( "-merge".equals(arg) ) {
				opts.dirMergeMethod = CcouchNamespace.REQ_DIRMERGE_MERGE;
			} else if( "-use-commit-targets".equals(arg) ) {
				opts.shouldUseCommitTargets = true;

			} else if( "-dump-config".equals(arg) ) {
				opts.shouldDumpConfig = true;
			} else if( "-h".equals(arg) || "-?".equals(arg) ) {
				opts.showHelp = true;
				return opts;
			
			// What to copy to/from
			} else if( arg.charAt(0) != '-' || "-".equals(arg) ) {
				opts.uris.add(arg);
				
			} else {
				System.err.println("ccouch " + commandName + ": Unrecognised argument: " + arg);
				return null;
			}
		}
		return opts;
	}
	
	//// Shared command routines ////
	
	protected int copy( String sourceUri, String destUri, GeneralOptions opts ) {
		Commit commit = null;
		Response commitRes = null;
		String sourceTargetUri = sourceUri;

		Response getRes = null;
		
		findTarget: while( true ) {
			BaseRequest getReq = new BaseRequest( Request.VERB_GET, sourceTargetUri );
			getRes = TheGetter.handleRequest(getReq);
			if( getRes.getStatus() != Response.STATUS_NORMAL ) {
				System.err.println("Couldn't get " + sourceUri + ": " + getRes.getContent());
				return 1;
			}
			if( getRes.getContent() == null ) {
				System.err.println("No content found at " + sourceUri);
				return 1;
			}
			if( opts.shouldUseCommitTargets && getRes.getContent() instanceof Commit ) {
				commit = (Commit)getRes.getContent();
				commitRes = getRes;
				Object o = commit.getTarget();
				if( o instanceof Ref ) {
					sourceTargetUri = ((Ref)o).getTargetUri();
					continue findTarget;
				} else {
					break findTarget;
				}
			} else {
				break findTarget;
			}
		}
		
		BaseRequest putReq = new BaseRequest( Request.VERB_PUT, destUri );
		putReq.content = getRes.getContent();
		putReq.contentMetadata = getRes.getContentMetadata();
		putReq.putMetadata(CcouchNamespace.REQ_STORE_SECTOR, opts.storeSector);
		if( opts.shouldLinkStored ) putReq.putMetadata(CcouchNamespace.REQ_HARDLINK_DESIRED, Boolean.TRUE);
		if( opts.shouldRelinkImported ) putReq.putMetadata(CcouchNamespace.REQ_REHARDLINK_DESIRED, Boolean.TRUE);
		putReq.putMetadata(CcouchNamespace.REQ_DIRMERGE_METHOD, opts.dirMergeMethod);
		putReq.putMetadata(CcouchNamespace.REQ_FILEMERGE_METHOD, opts.fileMergeMethod);
		Response putRes = TheGetter.handleRequest(putReq);
		if( putRes.getStatus() != Response.STATUS_NORMAL ) {
			System.err.println("Couldn't PUT to " + destUri + ": " + putRes.getContent());
			return 1;
		}
		if( commit != null && opts.shouldSaveCommitUri ) {
			String commitListUri = getCommitListUri(destUri);
			if( commitListUri != null ) {
				addCommitUri(commitListUri, TheGetter.identify( commit, commitRes.getContentMetadata() ) );
			}
		}
		return 0;
	}
	
	//// Commands ////	

	public int runCopyCmd( String[] args ) {
		GeneralOptions opts = getGeneralOptions(args, "copy");
		if( opts.showHelp ) {
			System.out.println(COPY_USAGE);
			return 0;
		}
		if( opts == null ) {
			System.err.println(COPY_USAGE);
			return 1;
		}
		
		if( opts.uris.size() <= 1 ) {
			System.err.println("Must specify at least source and dest");
			System.err.println(COPY_USAGE);
			return 1;
		}
		
		int errorCount = 0;

		List uris = new ArrayList(opts.uris);
		String destUri = normalizeUri((String)uris.remove(uris.size()-1), true, false);
		
		for( Iterator i=uris.iterator(); i.hasNext(); ) {
			String sourceUri = normalizeUri((String)i.next(), false, false);
			
			errorCount += copy( sourceUri, destUri, opts );
		}
		return errorCount;
	}
	
	public int runIdCmd( String[] args ) {
		args = mergeConfiguredArgs("id", args);
		List inputUris = new ArrayList();
		boolean reportInputs = true;
		boolean dumpConfig = false;
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( "-hide-inputs".equals(arg) ) {
				reportInputs = false;
			} else if( "-?".equals(arg) || "-h".equals(arg) ) {
				System.out.println(ID_USAGE);
				return 0;
			} else if( "-dump-config".equals(arg) ) {
				dumpConfig = true;
			} else if( !arg.startsWith("-") || "-".equals(arg) ) {
				inputUris.add(normalizeUri(arg, false, false));
			} else {
				System.err.println("ccouch cache-heads: Unrecognised argument: " + arg);
				System.err.println(CACHE_HEADS_USAGE);
				return 1;
			}
		}
		
		if( dumpConfig ) {
			dumpRepoConfig(metaRepoConfig, System.out, "");
			return 0;
		}

		int errorCount = 0;
		for( Iterator i=inputUris.iterator(); i.hasNext(); ) {
			String input = (String)i.next();
			BaseRequest getReq = new BaseRequest(Request.VERB_GET, input);
			Response getRes = TheGetter.handleRequest(getReq);
			if( getRes.getStatus() != Response.STATUS_NORMAL ) {
				System.err.println("Couldn't find " + getReq.getUri() + ": " + getRes.getStatus() + ": " + getRes.getContent() );
				++errorCount;
			} else {
				String id = TheGetter.identify( getRes.getContent(), getRes.getContentMetadata() );
				if( reportInputs ) {
					System.out.println( input + "\t" + id );
				} else {
					System.out.println( id );
				}
			}
		}
		return errorCount;
	}
	
	public int runStoreCmd( String[] args ) {
		args = mergeConfiguredArgs("store", args);
		List sourceUris = new ArrayList();
		String message = null;
		String name = null;
		String author = null;
		boolean storeDirs = true;
		boolean forceCommit = false;
		boolean shouldLinkStored = false;
		boolean shouldRelinkImported = false;
		boolean followRefs = false;
		String storeSector = "user";
		for( int i=0; i < args.length; ++i ) {
			String arg = args[i];
			if( arg.length() == 0 ) {
				System.err.println(STORE_USAGE);
				return 1;
			} else if( "-files-only".equals(arg) ) {
				storeDirs = false;
			} else if( "-v".equals(arg) ) {
				Log.setLevel(Log.LEVEL_CHATTY);
			} else if( "-link".equals(arg) ) {
				shouldLinkStored = true;
			} else if( "-relink".equals(arg) ) {
				shouldLinkStored = true;
				shouldRelinkImported = true;
			} else if( "-store-sector".equals(arg) ) {
				storeSector = args[++i];
			} else if( "-follow-refs".equals(arg) ) {
				followRefs = true;
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
				return 0;
			} else if( arg.charAt(0) != '-' || "-".equals(arg) ) {
				sourceUris.add(normalizeUri(arg,false,false));
			} else {
				System.err.println("ccouch store: Unrecognised argument: " + arg);
				System.err.println(STORE_USAGE);
				return 1;
			}
		}
		
		boolean createCommit = forceCommit || (author != null) || (name != null) || (message != null);
		int errorCount = 0;
		
		if( createCommit ) {
			if( sourceUris.size() != 1 ) {
				System.err.println("When creating a commit, exactly one source must be specified");
				return 1;
			}
		}
		
		String dataDestUri = "x-ccouch-repo:data/" + (storeSector != null ? storeSector + "/" : "");
		String storedUri = null;
		for( Iterator i=sourceUris.iterator(); i.hasNext(); ) {
			String sourceUri = (String)i.next();
			
			Request getReq = new BaseRequest(Request.VERB_GET, sourceUri);
			Response getRes = TheGetter.handleRequest(getReq);
			if( getRes.getStatus() != Response.STATUS_NORMAL ) {
				System.err.println("Couldn't get " + sourceUri + ": " + getRes.getContent());
				++errorCount;
				continue;
			}
			if( getRes.getContent() == null ) {
				System.err.println("No content found at " + sourceUri);
				++errorCount;
				continue;
			}
			
			Object o = getRes.getContent();
			boolean expectIdentifier = true; 
			if( o instanceof Directory ) {
				if( !storeDirs ) {
					o = new FilterIterator( new DirectoryWalker((Directory)o, followRefs), EntryFilters.BLOBFILTER );
					getRes = new BaseResponse(Response.STATUS_NORMAL, o);
					expectIdentifier = false;
					if( createCommit ) {
						System.err.println("Cannot create commit when source is a collection");
						return 1;
					}
				}
			}
			
			BaseRequest putReq = new BaseRequest(Request.VERB_PUT, dataDestUri);
			putReq.content = getRes.getContent();
			putReq.contentMetadata = getRes.getContentMetadata(); 
			if( shouldLinkStored ) putReq.putMetadata(CcouchNamespace.REQ_HARDLINK_DESIRED, Boolean.TRUE);
			if( shouldRelinkImported ) putReq.putMetadata(CcouchNamespace.REQ_REHARDLINK_DESIRED, Boolean.TRUE);
			putReq.putMetadata(CcouchNamespace.REQ_FILEMERGE_METHOD, CcouchNamespace.REQ_FILEMERGE_STRICTIG);
			Response putRes = TheGetter.handleRequest(putReq);
			if( putRes.getStatus() != Response.STATUS_NORMAL ) {
				System.err.println("Couldn't PUT to " + dataDestUri + ": " + putRes.getContent());
				++errorCount;
			}
			if( expectIdentifier ) {
				storedUri = MetadataUtil.getStoredIdentifier(putRes);
				if( storedUri == null ) {
					Log.log(Log.LEVEL_ERRORS, Log.TYPE_ERROR, "Did not recieve identifier after storing " + sourceUri);
					++errorCount;
				} else {
					Log.log(Log.LEVEL_CHANGES, Log.TYPE_GENERIC, sourceUri + "\t" + storedUri);
				}
			}
		}
		
		createCommit: if( createCommit ) {
			String sourceUri = (String)sourceUris.get(0);
			String parentCommitListUri = getCommitListUri(sourceUri);
			
			BaseRef[] parents;
			if( parentCommitListUri != null ) {
				List parentCommitUris = getCommitUris(parentCommitListUri);

				for( int i=0; i<parentCommitUris.size(); ++i ) {
					String parentCommitUri = (String)parentCommitUris.get(i);
					if( !forceCommit ) {
						BaseRequest parentCommitRequest = new BaseRequest(Request.VERB_GET, parentCommitUri);
						Response parentCommitResponse = TheGetter.handleRequest(parentCommitRequest);
						if( parentCommitResponse.getStatus() == Response.STATUS_NORMAL ) {
							if( parentCommitResponse.getContent() instanceof Commit ) {
								Commit parentCommit = (Commit)parentCommitResponse.getContent();
								if( parentCommit.getTarget() instanceof Ref && storedUri.equals(((Ref)parentCommit.getTarget()).getTargetUri()) ) {
									System.err.println("Parent commit " + parentCommitUri + " references the same target as the new commit would: " + sourceUri + " = " + storedUri );
									System.err.println("Use -force-commit to commit anyway.");
									break createCommit;
								}
							} else { 
								Log.log(Log.LEVEL_WARNINGS, Log.TYPE_WARNING, parentCommitUri + " (listed in " + parentCommitListUri + ") does not reference a Commit.  Ignoring.");
								System.err.println("Warning: ");
							}
						} else {
							Log.log(Log.LEVEL_WARNINGS, Log.TYPE_WARNING, "Could not load commit " + parentCommitUri + "(listed in " + parentCommitListUri + ").  Ignoring.");
						}
					}
				}

				parents = new BaseRef[parentCommitUris.size()];
				for( int i=0; i<parents.length; ++i ) {
					parents[i] = new BaseRef((String)parentCommitUris.get(i));
				}
			} else {
				parents = new BaseRef[]{};
			}

			SimpleCommit commit = new SimpleCommit();
			commit.author = author;
			commit.date = new Date();
			commit.message = message;
			commit.target = new BaseRef(storedUri);
			commit.parents = parents;
			
			// Data already stored, so we don't really need to worry about rdfifying, here
			RdfCommit rdfCommit = new RdfCommit(commit, metaRepoConfig.getMetaRepository().getTargetRdfifier(false,false));
			
			String commitDestUri;
			if( name != null ) {
				commitDestUri = "x-ccouch-repo:heads/" + metaRepoConfig.defaultRepoConfig.name + "/" + name + "/new";
			} else {
				commitDestUri = dataDestUri;
			}
			
			BaseRequest storeCommitReq = new BaseRequest(Request.VERB_PUT, commitDestUri);
			storeCommitReq.content = BlobUtil.getBlob(rdfCommit.toString());
			Response storeCommitRes = TheGetter.handleRequest(storeCommitReq);
			if( storeCommitRes.getStatus() != Response.STATUS_NORMAL ) {
				Log.log(Log.LEVEL_ERRORS, Log.TYPE_ERROR, "Could not PUT commit to " + commitDestUri + ": " + BaseResponse.getErrorSummary(storeCommitRes));
				++errorCount;
				break createCommit;
			}
			String commitUrn = MetadataUtil.getStoredIdentifier(storeCommitRes);
			if( commitUrn == null ) {
				Log.log(Log.LEVEL_ERRORS, Log.TYPE_ERROR, "Did not recieve identifier after storing commit.");
				++errorCount;
				break createCommit;
			}
			commitUrn = "x-parse-rdf:" + commitUrn;
			
			Log.log(Log.LEVEL_CHANGES, Log.TYPE_GENERIC, "Stored commit as " + commitUrn);

			if( parentCommitListUri != null ) {
				Response storeCommitUriRes = writeCommitUri(parentCommitListUri, commitUrn);
				if( storeCommitUriRes.getStatus() != Response.STATUS_NORMAL ) {
					Log.log(Log.LEVEL_WARNINGS, Log.TYPE_WARNING, "Could not PUT new commit URI list to " + parentCommitListUri + ": " + BaseResponse.getErrorSummary(storeCommitUriRes));
					break createCommit;
				}
			}
		} // end create commit

		return errorCount;
	}
	
	public int runCheckoutCmd( String[] args ) {
		GeneralOptions opts = getGeneralOptions( args, "checkout" );
		if( opts == null ) {
			System.err.println(CHECKOUT_USAGE);
			return 1;
		}
		if( opts.showHelp ) {
			System.out.println(CHECKOUT_USAGE);
			return 0;
		}
		opts.shouldSaveCommitUri = true;
		opts.shouldUseCommitTargets = true;
		
		if( opts.uris.size() != 2 ) {
			System.err.println("ccouch checkout: You must specify one source and one destination URI");
			System.err.println(CHECKOUT_USAGE);
			return 1;
		}
		String sourceUri = normalizeUri((String)opts.uris.get(0), false, true);
		String destUri = normalizeUri((String)opts.uris.get(1), true, true);

		return copy( sourceUri, destUri, opts );
	}
	
	/** This is almost exactly the same as 'store', but with
	 *  fewer features and a different default store sector */
	public int runCacheCmd( String[] args ) {
		args = mergeConfiguredArgs("cache", args);
		
		GeneralOptions opts = new GeneralOptions();
		opts.storeSector = "remote";
		List cacheUris = new ArrayList();
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( "-q".equals(arg) ) {
			} else if( "-v".equals(arg) ) {
				Log.setLevel(Log.LEVEL_CHATTY);
			} else if( "-store-sector".equals(arg) ) {
				opts.storeSector = args[++i];
			} else if( "-link".equals(arg) ) {
				opts.shouldLinkStored = true;
			} else if( "-relink".equals(arg) ) {
				opts.shouldLinkStored = true;
				opts.shouldRelinkImported = true;
			} else if( "-?".equals(arg) || "-h".equals(arg) ) {
				System.out.println(CACHE_USAGE);
				return 0;
			} else if( !arg.startsWith("-") ) {
				cacheUris.add(arg);
			} else {
				System.err.println("ccouch cache: Unrecognised argument: " + arg);
				System.err.println(CACHE_USAGE);
				return 1;
			}
		}
		
		for( Iterator i=cacheUris.iterator(); i.hasNext(); ) {
			String uri = (String)i.next();
			copy( uri, "x-ccouch-repo:data", opts );
		}

		return 0;
	}
	
	public int runCacheHeadsCmd( String[] args ) {
		args = mergeConfiguredArgs("cache-heads", args);
		List cacheUris = new ArrayList();
		GeneralOptions opts = new GeneralOptions();
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( "-q".equals(arg) ) {
				Log.setLevel(Log.LEVEL_SILENT);
			} else if( "-v".equals(arg) ) {
				Log.setLevel(Log.LEVEL_CHATTY);
			} else if( "-?".equals(arg) || "-h".equals(arg) ) {
				System.out.println(CACHE_HEADS_USAGE);
				return 0;
			} else if( "-link".equals(arg) ) {
				opts.shouldLinkStored = true;
			} else if( "-relink".equals(arg) ) {
				opts.shouldLinkStored = true;
				opts.shouldRelinkImported = true;
			} else if( !arg.startsWith("-") ) {
				cacheUris.add(arg);
			} else {
				System.err.println("ccouch cache-heads: Unrecognised argument: " + arg);
				System.err.println(CACHE_HEADS_USAGE);
				return 1;
			}
		}
		
		int errorCount = 0;
		
		eachUrl: for( Iterator i=cacheUris.iterator(); i.hasNext(); ) {
			String uri = (String)i.next();
			RepoRef rr = RepoRef.parse(uri, true);
			String headPath = rr.getHeadPath();
			if( headPath == null ) {
				System.err.println(uri + " could not be parsed as a head path; try [x-ccouch-head:]//<repo>/<head-path>");
				++errorCount;
				continue eachUrl;
			}
			int headPathSlashIdx = headPath.indexOf('/');
			if( headPathSlashIdx == -1 ) {
				System.err.println(uri + " does not have enough head path components (try adding a '/' at the end)");
				++errorCount;
				continue eachUrl;
			}
			
			if( rr.repoName == null ) {
				rr.repoName = headPath.substring(0, headPathSlashIdx);
			}
			
			copy( rr.toString(), "x-ccouch-repo:/" + rr.subPath, opts );
		}
		
		return errorCount;
	}
	
	public int runCheckCmd( String[] args ) {
		args = mergeConfiguredArgs("check", args);
		List checkPaths = new ArrayList();
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( arg.startsWith("-" ) ) {
				System.err.println("ccouch check: Unrecognised argument: " + arg);
				System.err.println(CACHE_CHECK_USAGE);
				return 1;
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
		return 0;
	}
	
	public int runRdfifyCmd( String[] args ) {
		args = mergeConfiguredArgs("rdfify", args);
		String dir = null;
		boolean nested = false;
		boolean followRefs = false;
		for( int i=0; i < args.length; ++i ) {
			String arg = args[i];
			if( arg.length() == 0 ) {
				System.err.println(RDFIFY_USAGE);
				return 1;
			} else if( "-nested".equals(arg) ) {
				nested = true;
			} else if( "-follow-refs".equals(arg) ) {
				followRefs = true;
			} else if( arg.charAt(0) != '-' ) {
				dir = normalizeUri(arg, false, true);
			} else {
				System.err.println("ccouch rdfify: Unrecognised argument: " + arg);
				System.err.println(RDFIFY_USAGE);
				return 1;
			}
		}
		
		if( dir == null ) {
			System.err.println("No directory specified");
			System.err.println(RDFIFY_USAGE);
			return 1;
		}

		Object o = TheGetter.get(dir);
		if( !(o instanceof Directory) ) {
			System.err.println( dir + " does not point to a Directory (found " + (o == null ? "null" : "a " + o.getClass().getName()) + ")");
			return 1;
		}
		Directory d = (Directory)o;
		RdfDirectory rdf = new RdfDirectory(d, metaRepoConfig.getMetaRepository().getTargetRdfifier(nested, followRefs));
		System.out.println(rdf.toString());
		return 0;
	}
	
	public int run( String[] args ) {
		if( args.length == 0 ) {
			System.err.println(USAGE);
			return 1;
		}

		TheGetter.globalInstance = metaRepoConfig.getRequestKernel();
		InternalStreamRequestHandler.getInstance().addInputStream("stdin",System.in);
		InternalStreamRequestHandler.getInstance().addOutputStream("stdout",System.out);
		
		String cmd = null;
		int i;
		for( i=0; i<args.length; ) {
			int ni;
			if( "-h".equals(args[i]) || "-?".equals(args[i]) ) {
				System.out.println(USAGE);
				return 0;
			} else if( (ni = metaRepoConfig.handleArguments(args, i, "./")) > i ) {
				i = ni;
			} else if( args[i].length() > 0 && args[i].charAt(0) != '-' ) {
				cmd = args[i++];
				break;
			} else {
				System.err.println("ccouch: Unrecognised command: " + args[i]);
				System.err.println(USAGE);
				return 1;
			}
		}
		if( cmd == null ) { 
			System.err.println("No command given");
			System.err.println(USAGE);
			return 1;
		}
		String[] cmdArgs = new String[args.length-i];
		for( int j=0; j<cmdArgs.length; ++i, ++j ) {
			cmdArgs[j] = args[i];
		}
		
		int errorCount = 0;
		if( "config".equals(cmd) ) {
			System.out.println("Repo configuration:");
			errorCount += dumpRepoConfig( metaRepoConfig, System.out, "  ");
		} else if( "copy".equals(cmd) || "cp".equals(cmd) ) {
			errorCount += runCopyCmd( cmdArgs );
		} else if( "store".equals(cmd) ) {
			errorCount += runStoreCmd( cmdArgs );
		} else if( "checkout".equals(cmd) ) {
			errorCount += runCheckoutCmd( cmdArgs );
		} else if( "cache".equals(cmd) ) {
			errorCount += runCacheCmd( cmdArgs );
		} else if( "cache-heads".equals(cmd) ) {
			errorCount += runCacheHeadsCmd( cmdArgs );
		} else if( "check".equals(cmd) ) {
			errorCount += runCheckCmd( cmdArgs );
		} else if( "id".equals(cmd) ) {
			errorCount += runIdCmd( cmdArgs );
		} else if( "rdfify".equals(cmd) ) {
			errorCount += runRdfifyCmd( cmdArgs );
		} else {
			System.err.println("ccouch: Unrecognised sub-command: " + cmd);
			System.err.println(USAGE);
			return 1;
		}
		return errorCount;
	}
	
	public static void main( String[] args ) {
		int errorCount = new ContentCouchCommand().run( args );
		if( errorCount > 0 ) {
			System.err.println(errorCount + " errors occured");
			System.exit(1);
		}
	}
}
