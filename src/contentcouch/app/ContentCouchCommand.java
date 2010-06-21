// -*- tab-width:4 -*-
package contentcouch.app;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import contentcouch.app.Linker.LinkException;
import contentcouch.app.help.ContentCouchCommandHelp;
import contentcouch.blob.BlobUtil;
import contentcouch.commit.SimpleCommit;
import contentcouch.context.Config;
import contentcouch.context.Context;
import contentcouch.directory.DirectoryWalker;
import contentcouch.directory.EntryFilters;
import contentcouch.directory.FilterIterator;
import contentcouch.file.FileBlob;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.path.PathUtil.Path;
import contentcouch.rdf.CCouchNamespace;
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
	protected MetaRepoConfig metaRepoConfig = new MetaRepoConfig();
	
	protected String getHelpText( String topic ) {
		return ContentCouchCommandHelp.getString(topic);
	}
	
	protected String getHelpText() {
		return getHelpText("ccouch");
	}

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
	
	protected void initGlobalContext() {
		if( metaRepoConfig != null ) {
			Context.globalInstance.putAll(metaRepoConfig.config);
		}
	}
	
	Pattern DELTATIME_PATTERN = Pattern.compile("^(\\+|\\-)(\\d+)(\\w*)$");
	
	protected Date parseDeltaTime(Date relativeTo, String d) {
		Matcher m = DELTATIME_PATTERN.matcher(d); 
		if( m.matches() ) {
			String sign = m.group(1);
			long amount = Long.parseLong(m.group(2));
			String unit = m.group(3);
			long multiplier;
			if( "".equals(unit) || "seconds".equals(unit) || "s".equals(unit) ) {
				multiplier = 1000;
			} else if( "minutes".equals(unit) ) {
				multiplier = (long)(1000l*60);
			} else if( "hours".equals(unit) ) {
				multiplier = (long)(1000l*3600);
			} else if( "days".equals(unit) ) {
				multiplier = (long)(1000l*3600*24);
			} else if( "weeks".equals(unit) ) {
				multiplier = (long)(1000l*3600*24*7);
			} else if( "months".equals(unit) ) {
				multiplier = (long)(1000l*3600*24*30.4167);
			} else if( "years".equals(unit) ) {
				multiplier = (long)(1000l*3600*24*365.2425);
			} else {
				throw new RuntimeException("Unrecognised unit: " + unit);
			}
			if( "-".equals(sign) ) multiplier *= -1;
			return new Date(relativeTo.getTime() + amount*multiplier);
		} else {
			throw new RuntimeException( "Badly formatted time delta: " + d );
		}
	}
	
	protected class BaseArgumentHandler extends MultiArgumentHandler {
		public boolean helpShown;
		public int handleAllArguments( Iterator it ) {
			while( it.hasNext() ) {
				String current = (String)it.next();
				if( !handleArguments(current,it) ) return 1;
				if( helpShown ) return -1;
			}
			return 0;
		}
		public int handleAllArguments( String[] args ) {
			return handleAllArguments( Arrays.asList( args ).iterator());
		}
	}
	
	protected BaseArgumentHandler createArgumentHandler( final String commandName, final String helpDocName ) {
		final BaseArgumentHandler mah = new BaseArgumentHandler();
		mah.addArgumentHandler(new ArgumentHandler() {
			public boolean handleArguments(String current, Iterator rest) {
				if( "-h".equals(current) || "-?".equals(current) ) {
					System.out.println( getHelpText(helpDocName) );
					mah.helpShown = true;
					return true;
				} else {
					System.err.println( "ccouch " + commandName + ": Unrecognised argument '" + current + "'");
					System.err.println( getHelpText(helpDocName) );
					return false;
				}
			}
		});
		return mah;
	}
	
	protected BaseArgumentHandler createArgumentHandler( final String commandName ) {
		return createArgumentHandler( commandName, commandName );
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
		
		ps.println(pfx);
		ps.println(pfx + "Default command arguments:");
		for( Iterator i=mrc.cmdArgs.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			String cmdName = (String)e.getKey();
			List args = (List)e.getValue();
			ps.print(pfx + "  " + cmdName + " ");
			for( int j=0; j<args.size(); ++j ) {
				ps.print(args.get(j));
			}
			ps.println();
		}
		
		ps.println();
		ps.println(pfx+"Other parameters:");
		for( Iterator i=mrc.config.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			ps.println( pfx + "  " + e.getKey().toString() + " = " + e.getValue().toString() );
		}
		
		return 0;
	}
	
	//// Commit tracking ////
	
	protected String getCommitListUri(String checkoutUri) {
		checkoutUri = PathUtil.maybeFixFileDirectoryUri(checkoutUri);
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
		
		BaseRequest storeCommitUriReq = TheGetter.createRequest(RequestVerbs.VERB_PUT, commitListUri);
		storeCommitUriReq.putMetadata(CCouchNamespace.REQ_FILEMERGE_METHOD, CCouchNamespace.REQ_FILEMERGE_REPLACE);
		storeCommitUriReq.content = text;
		return TheGetter.call(storeCommitUriReq);
	}
	
	protected Response writeCommitUri( String commitListUri, String commitUri ) {
		TreeSet commitUris = new TreeSet();
		commitUris.add(commitUri);
		return writeCommitUris( commitListUri, commitUris );
	}
	
	protected List getCommitUris( String commitListUri, GeneralOptions opts ) {
		// Load parent commits
		BaseRequest parentCommitListReq = TheGetter.createRequest(RequestVerbs.VERB_GET, commitListUri);
		if( opts.cacheSector != null ) parentCommitListReq.putMetadata( CCouchNamespace.REQ_CACHE_SECTOR, opts.cacheSector );
		Response parentCommitListRes = TheGetter.call(parentCommitListReq);
		List commitUris = new ArrayList();
		if( parentCommitListRes.getStatus() == ResponseCodes.RESPONSE_NORMAL ) {
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
	
	protected Response addCommitUri( String commitListUri, String commitUri, GeneralOptions opts ) {
		TreeSet commitUris = new TreeSet(getCommitUris( commitListUri, opts ));
		commitUris.add(commitUri);
		return writeCommitUris( commitListUri, commitUris );
	}
	
	protected String normalizeUri( String uriOrPathOrSomething, boolean output, boolean directoryize ) {
		if( "-".equals(uriOrPathOrSomething) ) {
			return output ? "x-internal-stream:stdout" : "x-internal-stream:stdin";
		}
		if( directoryize && uriOrPathOrSomething.matches("^https?://.*/$") ) {
			return "active:contentcouch.directoryize+operand@" + UriUtil.uriEncode(uriOrPathOrSomething);
		}
		if( uriOrPathOrSomething.matches("^[A-Za-z]:[\\\\/].*") ) {
			return "file:///" + UriUtil.uriEncode(uriOrPathOrSomething.replace('\\', '/'));
		} else if( PathUtil.isUri(uriOrPathOrSomething) ) {
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
	
	protected static class GeneralOptions implements ArgumentHandler {
		public boolean shouldLinkStored;
		public boolean shouldDumpConfig;
		public boolean shouldSaveCommitUri; 
		public boolean shouldUseCommitTargets = false;
		public boolean shouldCreateUriDotFiles = false;
		public boolean shouldUseUriDotFiles = false;
		public Date shouldntCreateUriDotFilesWhenHighestBlobMtimeGreaterThan = null;
		public String storeSector;
		public String cacheSector;
		public String fileMergeMethod = CCouchNamespace.REQ_FILEMERGE_STRICTIG;
		public String dirMergeMethod = null;
		public int logLevel = Log.LEVEL_DOWNLOADS;
		HashSet extraLogEvents = new HashSet();
		
		public void setUpLogging() {
			Log.setStandardLogLevel(logLevel);
			for( Iterator i=extraLogEvents.iterator(); i.hasNext(); ) {
				Log.addLogger( (String)i.next(), Log.getStderrLogger() );
			}
		}
		
		public boolean handleArguments( String arg, Iterator it ) {
			if( arg.length() == 0 ) {
				
			// Linking options
			} else if( "-link".equals(arg) ) {
				this.shouldLinkStored = true;
			} else if( "-sector".equals(arg) ) {
				this.storeSector = this.cacheSector = (String)it.next();
			} else if( "-store-sector".equals(arg) ) {
				this.storeSector = (String)it.next();
			} else if( "-cache-sector".equals(arg) ) {
				this.cacheSector = (String)it.next();
				
			// Merging options:
			} else if( "-file-merge-method".equals(arg) ) {
				this.fileMergeMethod = (String)it.next();
			} else if( "-dir-merge-method".equals(arg) ) {
				this.dirMergeMethod = (String)it.next();
			} else if( "-replace-existing".equals(arg) ) {
				this.fileMergeMethod = CCouchNamespace.REQ_FILEMERGE_REPLACE;
			} else if( "-keep-existing".equals(arg) ) {
				this.fileMergeMethod = CCouchNamespace.REQ_FILEMERGE_IGNORE;
			} else if( "-merge".equals(arg) ) {
				this.dirMergeMethod = CCouchNamespace.REQ_DIRMERGE_MERGE;
			
			} else if( "-use-commit-targets".equals(arg) ) {
				this.shouldUseCommitTargets = true;
			
			} else if( "-create-uri-dot-files".equals(arg) ) {
				this.shouldCreateUriDotFiles = true;
			} else if( "-use-uri-dot-files".equals(arg) ) {
				this.shouldUseUriDotFiles = true;
			
			} else if( "-dump-config".equals(arg) ) {
				this.shouldDumpConfig = true;

			} else if( "-q".equals(arg) ) {
				this.logLevel = Log.LEVEL_QUIET;
			} else if( arg.startsWith("-v") ) {
				String logLevelStr = arg.substring(2);
				if( logLevelStr.length() == 0 ) {
					this.logLevel = Log.LEVEL_VERBOSE;
				} else if( logLevelStr.matches("^\\d+$") ) {
					try {
						this.logLevel = Integer.parseInt(logLevelStr);
					} catch( NumberFormatException e ) {
						throw new RuntimeException(e);
					}
				} else {
					this.extraLogEvents.add(logLevelStr);
				}
				
			} else {
				return false;
			}
			
			return true;
		}
	}
	
	// TODO: Create option handling classes for each command 
	
	//// Shared command routines ////
	
	protected int copy( String sourceUri, String destUri, GeneralOptions opts ) {
		Commit commit = null;
		Response commitRes = null;
		String sourceTargetUri = sourceUri;

		Response getRes = null;
		
		findTarget: while( true ) {
			BaseRequest getReq = TheGetter.createRequest( RequestVerbs.VERB_GET, sourceTargetUri );
			getReq.putMetadata(CCouchNamespace.REQ_CACHE_SECTOR, opts.cacheSector);
			getRes = TheGetter.call(getReq);
			if( getRes.getStatus() != ResponseCodes.RESPONSE_NORMAL ) {
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
		
		BaseRequest putReq = TheGetter.createRequest( RequestVerbs.VERB_PUT, destUri );
		putReq.content = getRes.getContent();
		putReq.contentMetadata = getRes.getContentMetadata();
		//putReq.putContextVar(SwfNamespace.CTX_CONFIG, metaRepoConfig.config);
		putReq.putContentMetadata(CCouchNamespace.SOURCE_URI, sourceUri);
		putReq.putMetadata(CCouchNamespace.REQ_STORE_SECTOR, opts.storeSector);
		putReq.putMetadata(CCouchNamespace.REQ_CACHE_SECTOR, opts.cacheSector);
		if( opts.shouldLinkStored ) putReq.putMetadata(CCouchNamespace.REQ_HARDLINK_DESIRED, Boolean.TRUE);
		putReq.putMetadata(CCouchNamespace.REQ_DIRMERGE_METHOD, opts.dirMergeMethod);
		putReq.putMetadata(CCouchNamespace.REQ_FILEMERGE_METHOD, opts.fileMergeMethod);
		Response putRes = TheGetter.call(putReq);
		if( putRes.getStatus() != ResponseCodes.RESPONSE_NORMAL ) {
			System.err.println("Couldn't PUT to " + destUri + ": " + putRes.getContent());
			return 1;
		}
		if( commit != null && opts.shouldSaveCommitUri ) {
			String commitListUri = getCommitListUri(destUri);
			if( commitListUri != null ) {
				addCommitUri(commitListUri, TheGetter.identify( commit, commitRes.getContentMetadata() ), opts );
			}
		}
		return 0;
	}
	
	//// Commands ////	

	public int runCopyCmd( String[] args ) {
		GeneralOptions opts = new GeneralOptions();
		BaseArgumentHandler bah = createArgumentHandler("copy");
		bah.addArgumentHandler( opts );
		final ArrayList paths = new ArrayList();
		bah.addArgumentHandler( new ArgumentHandler() {
			public boolean handleArguments(String current, Iterator rest) {
				if( current.equals("-") || !current.startsWith("-") ) {
					paths.add(current);
					return true;
				}
				return false;
			}
		});
		int errorCount = bah.handleAllArguments(args);
		if( errorCount != 0 ) return errorCount;
		
		if( paths.size() <= 1 ) {
			System.err.println("Must specify at least source and dest");
			System.err.println();
			System.err.println(getHelpText("copy"));
			System.err.println();
			return 1;
		}
		
		String destUri = normalizeUri((String)paths.remove(paths.size()-1), true, false);
		
		for( Iterator i=paths.iterator(); i.hasNext(); ) {
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
				System.out.println(getHelpText("id"));
				return 0;
			} else if( "-dump-config".equals(arg) ) {
				dumpConfig = true;
			} else if( !arg.startsWith("-") || "-".equals(arg) ) {
				inputUris.add(normalizeUri(arg, false, false));
			} else {
				System.err.println("ccouch cache-heads: Unrecognised argument: " + arg);
				System.err.println();
				System.err.println(getHelpText("id"));
				System.err.println();
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
			BaseRequest getReq = TheGetter.createRequest(RequestVerbs.VERB_GET, input);
			Response getRes = TheGetter.call(getReq);
			if( getRes.getStatus() != ResponseCodes.RESPONSE_NORMAL ) {
				System.err.println("Couldn't find " + getReq.getResourceName() + ": " + getRes.getStatus() + ": " + getRes.getContent() );
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
	
	protected int relink( File f ) {
		BaseRequest idReq = TheGetter.createRequest(RequestVerbs.VERB_POST, "x-ccouch-repo:identify");
		idReq.content = new FileBlob(f);
		Response idRes = TheGetter.call(idReq);
		String id = (String)TheGetter.getResponseValue(idRes, idReq);
		if( id == null ) {
			Log.log(Log.EVENT_ERROR, "Failed to identify " + f);
			return 0;
		}
		
		BaseRequest getStoreFileReq = TheGetter.createRequest(RequestVerbs.VERB_GET, id);
		getStoreFileReq.putMetadata(CCouchNamespace.REQ_LOCAL_REPOS_ONLY, Boolean.TRUE);
		Response getStoreFileRes = TheGetter.call(getStoreFileReq);
		int status = getStoreFileRes.getStatus();
		if( status == ResponseCodes.RESPONSE_DOESNOTEXIST || status == ResponseCodes.RESPONSE_UNHANDLED ) {
			// skip it.
		} else if( status == ResponseCodes.RESPONSE_NORMAL ) {
			Object hopefullyFile = TheGetter.getResponseValue(getStoreFileRes, getStoreFileReq);
			if( hopefullyFile instanceof File ) {
				File target = (File)hopefullyFile;
				// woohoo, found it!  Can we link?
				File backup = new File(f.getPath() + ".ccouch-relink-backup");
				if( !f.renameTo(backup) ) {
					Log.log(Log.EVENT_ERROR, "Couldn't rename " + f + " to replace with hardlink");
					return 1;
				}
				try {
					Linker.getInstance().link(target, f);
					Log.log(Log.EVENT_REPLACED, target.getAbsolutePath(), f.getAbsolutePath()); 
					backup.delete();
				} catch( LinkException e ) {
					Log.log(Log.EVENT_ERROR, "Couldn't link " + f + " to " + target);
					backup.renameTo(f);
					return 1;
				}
			}
		}
		return 0;
	}
	
	protected int _relink( File f ) {
		if( f.isDirectory() ) {
			File[] childs = f.listFiles();
			int errorCount = 0;
			for( int i=0; i<childs.length; ++i ) {
				File k = childs[i];
				if( k.getName().startsWith(".") ) continue;
				_relink(k);
			}
			return errorCount;
		} else {
			return relink(f);
		}
	}
	
	public int runRelinkCmd( String[] args ) {
		ArrayList filenames = new ArrayList();
		GeneralOptions opts = new GeneralOptions();
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( "-q".equals(arg) ) {
				opts.logLevel = Log.LEVEL_QUIET;
			} else if( "-v".equals(arg) ) {
				opts.logLevel = Log.LEVEL_VERBOSE;
			} else if( !arg.startsWith("-") ) {
				filenames.add(arg);
			} else {
				System.err.println("ccouch relink: Unrecognised argument: " + arg);
				System.err.println();
				System.err.println(getHelpText("relink"));
				System.err.println();
				return 1;
			}
		}
		opts.setUpLogging();
		
		int errorCount = 0;
		for( Iterator i=filenames.iterator(); i.hasNext(); ) {
			errorCount += _relink(new File((String)i.next()));
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
		boolean followRefs = false;
		boolean reportInputs = true;
		GeneralOptions opts = new GeneralOptions();
		for( int i=0; i < args.length; ++i ) {
			String arg = args[i];
			if( arg.length() == 0 ) {
				System.err.println();
				System.err.println(getHelpText("store"));
				System.err.println();
				return 1;
			} else if( "-hide-inputs".equals(arg) ) {
				reportInputs = false;
			} else if( "-files-only".equals(arg) ) {
				storeDirs = false;
			} else if( "-q".equals(arg) ) {
				opts.logLevel = Log.LEVEL_QUIET;
			} else if( "-v".equals(arg) ) {
				opts.logLevel = Log.LEVEL_VERBOSE;
			} else if( "-link".equals(arg) ) {
				opts.shouldLinkStored = true;
			} else if( "-store-sector".equals(arg) || "-sector".equals(arg) ) {
				opts.storeSector = args[++i];
			} else if( "-follow-refs".equals(arg) ) {
				followRefs = true;
			} else if( "-create-uri-dot-files".equals(arg) ) {
				opts.shouldCreateUriDotFiles = true;
			} else if( "-use-uri-dot-files".equals(arg) ) {
				opts.shouldUseUriDotFiles = true;
			} else if( "-dcudfnt".equals(arg) ) {
				opts.shouldntCreateUriDotFilesWhenHighestBlobMtimeGreaterThan = parseDeltaTime(new Date(), args[++i]);
			} else if( "-m".equals(arg) ) {
				message = args[++i];
			} else if( "-n".equals(arg) ) {
				name = args[++i];
			} else if( "-a".equals(arg) ) {
				author = args[++i];
			} else if( "-force-commit".equals(arg) ) {
				forceCommit = true;
			} else if( "-h".equals(arg) || "-?".equals(arg) ) {
				System.out.println(getHelpText("store"));
				return 0;
			} else if( arg.charAt(0) != '-' || "-".equals(arg) ) {
				sourceUris.add(normalizeUri(arg,false,false));
			} else {
				System.err.println("ccouch store: Unrecognised argument: " + arg);
				System.err.println();
				System.err.println(getHelpText("store"));
				System.err.println();
				return 1;
			}
		}
		opts.setUpLogging();

		String commitDestUri = null;
		if( name != null ) {
			if( metaRepoConfig.defaultRepoConfig.name == null ) {
				System.err.println("ccouch store: Would not be able to create head for commit;");
				System.err.println("default repository is not named");
				return 1;
			}
			commitDestUri = "x-ccouch-repo:heads/" + metaRepoConfig.defaultRepoConfig.name + "/" + name + "/new";
		}
		
		boolean createCommit = forceCommit || (author != null) || (name != null) || (message != null);
		int errorCount = 0;
		
		if( createCommit ) {
			if( sourceUris.size() != 1 ) {
				System.err.println("When creating a commit, exactly one source must be specified");
				return 1;
			}
		}
		
		String dataDestUri = "x-ccouch-repo:data";;
		String storedUri = null;
		for( Iterator i=sourceUris.iterator(); i.hasNext(); ) {
			String sourceUri = (String)i.next();
			
			BaseRequest getReq = TheGetter.createRequest(RequestVerbs.VERB_GET, sourceUri);
			if( opts.cacheSector != null ) getReq.putMetadata(CCouchNamespace.REQ_CACHE_SECTOR, opts.cacheSector);
			Response getRes = TheGetter.call(getReq);
			if( getRes.getStatus() != ResponseCodes.RESPONSE_NORMAL ) {
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
					getRes = new BaseResponse(ResponseCodes.RESPONSE_NORMAL, o);
					expectIdentifier = false;
					if( createCommit ) {
						System.err.println("Cannot create commit when source is a collection");
						return 1;
					}
				}
			}
			
			BaseRequest putReq = TheGetter.createRequest(RequestVerbs.VERB_PUT, dataDestUri);
			putReq.content = o;
			putReq.contentMetadata = new HashMap(getRes.getContentMetadata());
			putReq.contentMetadata.put(CCouchNamespace.SOURCE_URI, sourceUri);
			if( opts.storeSector != null ) putReq.putMetadata(CCouchNamespace.REQ_STORE_SECTOR, opts.storeSector);
			if( opts.cacheSector != null ) putReq.putMetadata(CCouchNamespace.REQ_CACHE_SECTOR, opts.cacheSector);
			if( opts.shouldLinkStored ) putReq.putMetadata(CCouchNamespace.REQ_HARDLINK_DESIRED, Boolean.TRUE);
			if( opts.shouldCreateUriDotFiles ) putReq.putMetadata(CCouchNamespace.REQ_CREATE_URI_DOT_FILES, Boolean.TRUE);
			if( opts.shouldUseUriDotFiles ) putReq.putMetadata(CCouchNamespace.REQ_USE_URI_DOT_FILES, Boolean.TRUE);
			putReq.putMetadata(CCouchNamespace.REQ_FILEMERGE_METHOD, CCouchNamespace.REQ_FILEMERGE_IGNORE);
			putReq.putMetadata(CCouchNamespace.REQ_DONT_CREATE_URI_DOT_FILES_WHEN_HIGHEST_BLOB_MTIME_GREATER_THAN,
				opts.shouldntCreateUriDotFilesWhenHighestBlobMtimeGreaterThan
			);
			
			Response putRes = TheGetter.call(putReq);
			if( putRes.getStatus() != ResponseCodes.RESPONSE_NORMAL ) {
				System.err.println("Couldn't PUT to " + dataDestUri + ": " + putRes.getContent());
				++errorCount;
			}
			if( expectIdentifier ) {
				storedUri = MetadataUtil.getStoredIdentifier(putRes);
				if( storedUri == null ) {
					Log.log(Log.EVENT_ERROR, "Did not recieve identifier after storing " + sourceUri);
					++errorCount;
				} else {
					if( reportInputs ) {
						System.out.println( sourceUri + "\t" + storedUri );
					} else {
						System.out.println( storedUri );
					}
				}
			}
		}
		
		createCommit: if( createCommit ) {
			String sourceUri = (String)sourceUris.get(0);
			String parentCommitListUri = getCommitListUri(sourceUri);
			
			BaseRef[] parents;
			if( parentCommitListUri != null ) {
				List parentCommitUris = getCommitUris(parentCommitListUri, opts);

				for( int i=0; i<parentCommitUris.size(); ++i ) {
					String parentCommitUri = (String)parentCommitUris.get(i);
					if( !forceCommit ) {
						BaseRequest parentCommitRequest = TheGetter.createRequest(RequestVerbs.VERB_GET, parentCommitUri);
						if( opts.cacheSector != null ) parentCommitRequest.putMetadata(CCouchNamespace.REQ_CACHE_SECTOR, opts.cacheSector);
						Response parentCommitResponse = TheGetter.call(parentCommitRequest);
						if( parentCommitResponse.getStatus() == ResponseCodes.RESPONSE_NORMAL ) {
							if( parentCommitResponse.getContent() instanceof Commit ) {
								Commit parentCommit = (Commit)parentCommitResponse.getContent();
								if( parentCommit.getTarget() instanceof Ref && storedUri.equals(((Ref)parentCommit.getTarget()).getTargetUri()) ) {
									System.err.println("Parent commit " + parentCommitUri + " references the same target as the new commit would: " + sourceUri + " = " + storedUri );
									System.err.println("Use -force-commit to commit anyway.");
									break createCommit;
								}
							} else { 
								Log.log(Log.EVENT_WARNING, parentCommitUri + " (listed in " + parentCommitListUri + ") does not reference a Commit.  Ignoring.");
								System.err.println("Warning: ");
							}
						} else {
							Log.log(Log.EVENT_WARNING, "Could not load commit " + parentCommitUri + "(listed in " + parentCommitListUri + ").  Ignoring.");
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
			
			BaseRequest storeCommitReq = TheGetter.createRequest(RequestVerbs.VERB_PUT, dataDestUri);
			storeCommitReq.content = BlobUtil.getBlob(rdfCommit.toString());
			if( opts.storeSector != null ) storeCommitReq.putMetadata(CCouchNamespace.REQ_STORE_SECTOR, opts.storeSector);
			if( opts.cacheSector != null ) storeCommitReq.putMetadata(CCouchNamespace.REQ_CACHE_SECTOR, opts.cacheSector);
			if( opts.shouldLinkStored ) storeCommitReq.putMetadata(CCouchNamespace.REQ_HARDLINK_DESIRED, Boolean.TRUE);
			storeCommitReq.putMetadata(CCouchNamespace.REQ_FILEMERGE_METHOD, CCouchNamespace.REQ_FILEMERGE_STRICTIG);
			Response storeCommitRes = TheGetter.call(storeCommitReq);
			if( storeCommitRes.getStatus() != ResponseCodes.RESPONSE_NORMAL ) {
				Log.log(Log.EVENT_ERROR, "Could not PUT commit to " + dataDestUri + ": " + TheGetter.getResponseErrorSummary(storeCommitRes));
				++errorCount;
				break createCommit;
			}
			
			String commitBlobUrn = MetadataUtil.getStoredIdentifier(storeCommitRes);
			if( commitBlobUrn == null ) {
				Log.log(Log.EVENT_ERROR, "Did not recieve identifier after storing commit.");
				++errorCount;
				break createCommit;
			}

			String commitUrn = Config.getRdfSubjectPrefix() + commitBlobUrn;
			
			if( reportInputs ) {
				System.out.println( "New Commit\t" + commitUrn );
			} else {
				System.out.println( commitUrn );
			}

			if( parentCommitListUri != null ) {
				Response storeCommitUriRes = writeCommitUri(parentCommitListUri, commitUrn);
				if( storeCommitUriRes.getStatus() != ResponseCodes.RESPONSE_NORMAL ) {
					Log.log(Log.EVENT_WARNING, "Could not PUT new commit URI list to " + parentCommitListUri + ": " + TheGetter.getResponseErrorSummary(storeCommitUriRes));
					break createCommit;
				}
			}

			if( commitDestUri != null ) {
				BaseRequest storeCommitHeadReq = new BaseRequest(storeCommitReq, commitDestUri);
				storeCommitHeadReq.content = TheGetter.get(commitBlobUrn);
				Response storeCommitHeadRes = TheGetter.call(storeCommitHeadReq);
				if( storeCommitHeadRes.getStatus() != ResponseCodes.RESPONSE_NORMAL ) {
					Log.log(Log.EVENT_ERROR, "Could not PUT commit to " + commitDestUri + ": " + TheGetter.getResponseErrorSummary(storeCommitRes));
					++errorCount;
					break createCommit;
				}
			}
		} // end create commit

		return errorCount;
	}
	
	public int runCheckoutCmd( String[] args ) {
		GeneralOptions opts = new GeneralOptions();
		BaseArgumentHandler bah = createArgumentHandler("checkout");
		bah.addArgumentHandler( opts );
		final ArrayList paths = new ArrayList();
		bah.addArgumentHandler( new ArgumentHandler() {
			public boolean handleArguments(String current, Iterator rest) {
				if( current.equals("-") || !current.startsWith("-") ) {
					paths.add(current);
					return true;
				}
				return false;
			}
		});
		int errorCount = bah.handleAllArguments(args);
		if( errorCount != 0 ) return errorCount;

		opts.setUpLogging();
		opts.shouldSaveCommitUri = true;
		opts.shouldUseCommitTargets = true;
		
		if( paths.size() != 2 ) {
			System.err.println("ccouch checkout: You must specify one source and one destination URI");
			System.err.println(getHelpText("checkout"));
			System.err.println();
			return 1;
		}
		String sourceUri = normalizeUri((String)paths.get(0), false, true);
		String destUri = normalizeUri((String)paths.get(1), true, true);

		return copy( sourceUri, destUri, opts );
	}
	
	/** This is almost exactly the same as 'store', but with
	 *  fewer features and a different default store sector */
	public int runCacheCmd( String[] args ) {
		args = mergeConfiguredArgs("cache", args);
		
		GeneralOptions opts = new GeneralOptions();
		opts.storeSector = "remote";
		BaseArgumentHandler bah = createArgumentHandler("cache");
		bah.addArgumentHandler(opts);
		final ArrayList paths = new ArrayList();		
		bah.addArgumentHandler( new ArgumentHandler() {
			public boolean handleArguments(String current, Iterator rest) {
				if( current.equals("-") || !current.startsWith("-") ) {
					paths.add(current);
					return true;
				}
				return false;
			}
		});
		int errorCount = bah.handleAllArguments(args);
		if( errorCount != 0 ) return errorCount;
		
		opts.setUpLogging();

		for( Iterator i=paths.iterator(); i.hasNext(); ) {
			String uri = normalizeUri((String)i.next(), false, false);
			copy( uri, "x-ccouch-repo:data", opts );
		}

		return 0;
	}
	
	public int runCacheHeadsCmd( String[] args ) {
		args = mergeConfiguredArgs("cache-heads", args);
		List cacheUris = new ArrayList();
		GeneralOptions opts = new GeneralOptions();
		opts.dirMergeMethod = CCouchNamespace.REQ_DIRMERGE_MERGE;
		opts.fileMergeMethod = CCouchNamespace.REQ_FILEMERGE_IGNORE;
		
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( "-q".equals(arg) ) {
				opts.logLevel = Log.LEVEL_QUIET;
			} else if( "-v".equals(arg) ) {
				opts.logLevel = Log.LEVEL_VERBOSE;
			} else if( "-?".equals(arg) || "-h".equals(arg) ) {
				System.out.println(getHelpText("cache-heads"));
				return 0;
			} else if( "-link".equals(arg) ) {
				opts.shouldLinkStored = true;
			} else if( !arg.startsWith("-") ) {
				cacheUris.add(arg);
			} else {
				System.err.println("ccouch cache-heads: Unrecognised argument: " + arg);
				System.err.println();
				System.err.println(getHelpText("cache-heads"));
				System.err.println();
				return 1;
			}
		}
		opts.setUpLogging();
		
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
				System.err.println();
				System.err.println(getHelpText("check"));
				System.err.println();
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
			Path p = PathUtil.parseFilePathOrUri(path);
			if( p == null ) {
				System.err.println("Couldn't parse '"+path+"' as file path");
			} else {
				File f = new File(p.toString());
				rc.checkFiles(f);
			}
		}
		return 0;
	}
	
	public int runRdfifyCmd( String[] args ) {
		args = mergeConfiguredArgs("rdfify", args);
		String dir = null;
		boolean nested = false;
		boolean followRefs = false;
		GeneralOptions opts = new GeneralOptions();
		for( int i=0; i < args.length; ++i ) {
			String arg = args[i];
			if( arg.length() == 0 ) {
				System.err.println(getHelpText("rdfify"));
				System.err.println();
				return 1;
			} else if( "-nested".equals(arg) ) {
				nested = true;
			} else if( "-follow-refs".equals(arg) ) {
				followRefs = true;
			} else if( arg.charAt(0) != '-' ) {
				dir = normalizeUri(arg, false, true);
			} else {
				System.err.println("ccouch rdfify: Unrecognised argument: " + arg);
				System.err.println();
				System.err.println(getHelpText("rdfify"));
				System.err.println();
				return 1;
			}
		}
		
		Log.setStandardLogLevel(opts.logLevel);
		
		if( dir == null ) {
			System.err.println("No directory specified");
			System.err.println();
			System.err.println(getHelpText("rdfify"));
			System.err.println();
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
	
	protected void touch( File f ) {
		if( f.isDirectory() ) {
			File uriFile = new File(f + "/.ccouch-uri");
			if( uriFile.exists() ) {
				Log.log(Log.EVENT_DELETED, uriFile.getPath());
				uriFile.delete();				
			}
		}
	}
	protected void touchParents( File f ) {
		f = f.getParentFile();
		while( f != null ) {
			touch(f);
			f = f.getParentFile();
		}
	}
	protected void touchChildren( File f ) {
		if( f.isDirectory() ) {
			File[] subs = f.listFiles();
			for( int i=0; i<subs.length; ++i ) {
				File sub = subs[i];
				if( sub.isDirectory() ) {
					touch(sub);
					touchChildren(sub);
				}
			}
		}
	}
	
	public int runTouchCmd( String[] args ) {
		args = mergeConfiguredArgs("touch", args);
		boolean recursive = false;
		List uris = new ArrayList();
		GeneralOptions opts = new GeneralOptions();
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( arg.equals("-r") ) {
				recursive = true;
			} else if( "-?".equals(arg) || "-h".equals(arg) ) {
				System.out.println(getHelpText("touch"));
				return 0;
			} else if( !arg.startsWith("-") ) {
				uris.add(normalizeUri(arg, true, true));
			} else {
				System.err.println("ccouch touch: Unrecognised argument: " + arg);
				System.err.println();
				System.err.println(getHelpText("touch"));
				System.err.println();
				return 1;
			}
		}
		Log.setStandardLogLevel(opts.logLevel);
		int errorCount = 0;
		for( Iterator pi=uris.iterator(); pi.hasNext(); ) {
			String uri = (String)pi.next();
			if( !uri.startsWith("file:") ) {
				System.err.println("'touch' currently only supports file URIs, " + uri + " given");
				++errorCount;
				continue;
			}
			String filePath = PathUtil.parseFilePathOrUri(uri).toString();
			File f = new File(filePath);
			if( !f.exists() ) {
				System.err.println("File " + f + " does not exist");
				++errorCount;
				continue;
			}
			f = f.getAbsoluteFile();
			touch(f);
			if( recursive ) touchChildren(f);
			touchParents(f);				
		}
		return 0;
	}
	
	protected boolean getterInitialized = false;
	protected boolean initializeGetter() {
		if( !getterInitialized ) {
			TheGetter.globalInstance = metaRepoConfig.getRequestKernel();
			InternalStreamRequestHandler.getInstance().addInputStream("stdin",System.in);
			InternalStreamRequestHandler.getInstance().addOutputStream("stdout",System.out);
		}
		return getterInitialized = true;
	}
	
	public int run( String[] args ) {
		if( args.length == 0 ) {
			System.err.println(getHelpText());
			System.err.println();
			return 1;
		}
		
		String cmd = null;
		int i;
		for( i=0; i<args.length; ) {
			int ni;
			if( "-h".equals(args[i]) || "-?".equals(args[i]) ) {
				System.out.println(getHelpText());
				return 0;
			} else if( initializeGetter() && (ni = metaRepoConfig.handleArguments(args, i, "./")) > i ) {
				i = ni;
			} else if( args[i].length() > 0 && args[i].charAt(0) != '-' ) {
				cmd = args[i++];
				break;
			} else {
				System.err.println("ccouch: Unrecognised command: " + args[i]);
				System.err.println();
				System.err.println(getHelpText());
				System.err.println();
				return 1;
			}
		}
		if( cmd == null ) { 
			System.err.println("ccouch: No command given");
			System.err.println();
			System.err.println(getHelpText());
			System.err.println();
			return 1;
		}
		initializeGetter();
		String[] cmdArgs = new String[args.length-i];
		for( int j=0; j<cmdArgs.length; ++i, ++j ) {
			cmdArgs[j] = args[i];
		}
		
		initGlobalContext();
		
		int errorCount = 0;
		if( "help".equals(cmd) ) {
			String docName = "ccouch";
			if( cmdArgs.length > 0 ) {
				docName = cmdArgs[0];
			}
			String doc = ContentCouchCommandHelp.getString(docName);
			if( doc == null ) {
				System.err.println("ccouch: no help text for '"+docName+"'");
				return 1;
			}
			System.out.println(doc);
			return 0;
		} else if( "config".equals(cmd) ) {
			System.out.println("Repo configuration:");
			errorCount += dumpRepoConfig( metaRepoConfig, System.out, "  ");
		} else if( "copy".equals(cmd) || "cp".equals(cmd) ) {
			errorCount += runCopyCmd( cmdArgs );
		} else if( "relink".equals(cmd) ) {
			errorCount += runRelinkCmd( cmdArgs );
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
		} else if( "touch".equals(cmd) ) {
			errorCount += runTouchCmd( cmdArgs );
		} else {
			System.err.println("ccouch: Unrecognised sub-command: " + cmd);
			System.err.println();
			System.err.println(getHelpText());
			System.err.println();
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
