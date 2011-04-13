package contentcouch.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.value.Blob;
import contentcouch.app.Log;
import contentcouch.blob.BlobInputStream;
import contentcouch.file.FileRequestHandler;
import contentcouch.framework.MultiRequestHandler;
import contentcouch.framework.TheGetter;
import contentcouch.http.HttpRequestHandler;
import contentcouch.misc.MemTempRequestHandler;
import contentcouch.misc.UriUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.stream.InternalStreamRequestHandler;

public class MetaRepoConfig {
	public Map namedRepoConfigs = new HashMap();
	public Map cmdArgs = new HashMap();
	public Map config = new HashMap();
	public RepoConfig defaultRepoConfig = new RepoConfig();
	public List localRepoConfigs = new ArrayList();
	public List remoteRepoConfigs = new ArrayList();
	public List loadedFromConfigUris = new ArrayList(); 
	
	public List getDefaultAndLocalRepoConfigs() {
		ArrayList l = new ArrayList();
		l.add(defaultRepoConfig);
		l.addAll(localRepoConfigs);
		return l;
	}
	
	public List getAllRepoConfigs() {
		ArrayList l = new ArrayList();
		l.add(defaultRepoConfig);
		l.addAll(localRepoConfigs);
		l.addAll(remoteRepoConfigs);
		return l;
	}
	
	protected void addLocalRepo( RepoConfig cfg ) {
		localRepoConfigs.add(0,cfg);
	}
	
	protected void addRemoteRepo( RepoConfig cfg ) {
		remoteRepoConfigs.add(0,cfg);
	}

	static final String[] emptyArgString = new String[] {};
	public String[] getCommandArgs(String commandName) {
		List ca = (List)cmdArgs.get(commandName);
		if( ca == null ) return emptyArgString;
		String[] args = new String[ca.size()];
		for( int i=0; i<args.length; ++i ) {
			args[i] = (String)ca.get(i);
		}
		return args;
	}
	
	public void addRepoConfig( RepoConfig rp ) {
		if( rp.disposition == RepoConfig.DISPOSITION_DEFAULT ) {
			if( rp.name != null ) defaultRepoConfig.name = rp.name;
			if( rp.uri != null ) defaultRepoConfig.uri = rp.uri;
			String cfgUri = rp.uri + "ccouch-config";

			BaseRequest cfgRequest = new BaseRequest(RequestVerbs.GET, cfgUri);
			Response cfgResponse = TheGetter.call(cfgRequest);
			Blob cfgBlob;
			if( cfgResponse.getStatus() == ResponseCodes.NORMAL && (cfgBlob = (Blob)cfgResponse.getContent()) != null ) {
				BufferedReader brd = new BufferedReader(new InputStreamReader(new BlobInputStream(cfgBlob)));
				try {
					_loadConfig(brd, cfgUri);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		} else if( rp.disposition == RepoConfig.DISPOSITION_LOCAL ) {
			addLocalRepo(rp);
		} else if( rp.disposition == RepoConfig.DISPOSITION_CACHE ) {
			Log.log(Log.EVENT_WARNING, "Cache repo specified, but cache repositories are no longer used.  Treating as a local repo.");
			addLocalRepo(rp);
		} else if( rp.disposition == RepoConfig.DISPOSITION_REMOTE ) {
			addRemoteRepo(rp);
		} else {
			throw new RuntimeException("unknown repo disposition: " + rp.disposition);
		}
		if( rp.name != null ) namedRepoConfigs.put(rp.name, rp);
	}
	
	public static void initNewStyleConfig( Map config ) {
		config.put(CCouchNamespace.CFG_RDF_DIRECTORY_STYLE, CCouchNamespace.RDF_DIRECTORY_STYLE_NEW);
		config.put(CCouchNamespace.CFG_RDF_SUBJECT_URI_PREFIX, CCouchNamespace.RDF_SUBJECT_URI_PREFIX);
		config.put(CCouchNamespace.CFG_ID_SCHEME, "bitprint");
	}

	public static void initOldStyleConfig( Map config ) {
		config.put(CCouchNamespace.CFG_RDF_DIRECTORY_STYLE, CCouchNamespace.RDF_DIRECTORY_STYLE_OLD);
		config.put(CCouchNamespace.CFG_RDF_SUBJECT_URI_PREFIX, CCouchNamespace.RDF_SUBJECT_URI_PREFIX_OLD);
		config.put(CCouchNamespace.CFG_ID_SCHEME, "sha1");
	}
	
	public int handleArguments( String[] args, int offset, String baseUri ) {
		if( offset >= args.length ) return offset;
		String arg = args[offset];
		RepoConfig rp = RepoConfig.parse(arg);
		if( rp != null ) {
			++offset;
			rp.uri = PathUtil.maybeNormalizeFileUri(PathUtil.appendPath(baseUri, args[offset]));
			if( !rp.uri.endsWith("/") ) rp.uri += "/";
			if( rp.uri.matches("^https?:.*") ) {
				rp.uri = "active:contentcouch.directoryize+operand@" + UriUtil.uriEncode(rp.uri);
			}
			//System.err.println(basePath + " + " + args[offset] + " = " + path);
			++offset;

			addRepoConfig(rp);
		} else if( "-file".equals(arg) ) {
			++offset;
			String configFile = args[offset];
			++offset;
			Blob cfgBlob = (Blob)TheGetter.get(configFile);
			BufferedReader brd = new BufferedReader(new InputStreamReader(new BlobInputStream(cfgBlob)));
			try {
				_loadConfig(brd, configFile);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else if( "-repo-name".equals(arg) ) {
			++offset;
			String name = args[offset];
			++offset;
			defaultRepoConfig.name = name;
			namedRepoConfigs.put(name, defaultRepoConfig);
		} else if( "-config".equals(arg) ) {
			++offset;
			String key = args[offset];
			++offset;
			String value = args[offset];
			config.put(key, value);
			++offset;
		} else if( "-old-style".equals(arg) ) {
			initOldStyleConfig(config);
			++offset;
		} else if( "-new-style".equals(arg) ) {
			initNewStyleConfig(config);
			++offset;
		}
		return offset;
	}

	static Pattern argPattern = Pattern.compile("(?:\"(?:[^\\\\\"]|\\\\\\\\|\\\\\")*\"|\\S)+");
	
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
	
	protected void _loadConfig( BufferedReader fr, String configUri ) throws IOException {
		loadedFromConfigUris.add(configUri);
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
		while( (endupat = handleArguments( argar, offset, configUri )) > offset ) {
		    offset = endupat;
		}
		if( endupat < argar.length ) {
			System.err.println("Unrecognised arg in " + configUri + ": " + argar[endupat]);
		}
	}
	
	
	//// Set up stuff to make us useful ////
	
	protected MetaRepository metaRepositoryCache;
	protected MultiRequestHandler requestKernelCache;
	
	public MetaRepository getMetaRepository() {
		if( metaRepositoryCache == null ) {
			metaRepositoryCache = new MetaRepository(this);
		}
		return metaRepositoryCache;
	}
	
	public MultiRequestHandler getRequestKernel() {
		if( requestKernelCache == null ) {
			requestKernelCache = new MultiRequestHandler();
			requestKernelCache.addRequestHandler(new MemTempRequestHandler());
			requestKernelCache.addRequestHandler(new HttpRequestHandler());
			requestKernelCache.addRequestHandler(new FileRequestHandler());
			requestKernelCache.addRequestHandler(InternalStreamRequestHandler.getInstance());
			requestKernelCache.addRequestHandler(getMetaRepository());
			TheGetter.initializeBasicCallables(requestKernelCache);
		}
		return requestKernelCache;
	}
}
