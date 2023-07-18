package contentcouch.repository;

import contentcouch.contentaddressing.ContentAddressingScheme;
import contentcouch.contentaddressing.Sha1Scheme;
import contentcouch.misc.UriUtil;
import contentcouch.path.PathUtil;

public class RepoConfig {
	public static final String DISPOSITION_DEFAULT = "default";
	public static final String DISPOSITION_LOCAL = "local";
	public static final String DISPOSITION_CACHE = "cache";
	public static final String DISPOSITION_REMOTE = "remote";
	
	public String disposition = DISPOSITION_DEFAULT;
	public final String name;
	/** Path of the root of the repository - should always end with '/' */
	public final String rawUri;
	/** URI of repository such that "/"-ending paths will resolve to directories.
	 * For HTTP URLs, this means wrapping in an active:contentcouch.directoryize URI. */
	public final String directoryizedUri;
	public String userStoreSector   = "user";
	public String remoteCacheSector = "remote";
	public String activeCacheSector = "active";
	public ContentAddressingScheme storageScheme = Sha1Scheme.getInstance();
	
	public RepoConfig( String disposition, String name, String rawUri ) {
		this.disposition = disposition;
		this.name = name;
		this.rawUri = rawUri;
		if( !rawUri.contains(":") ) throw new RuntimeException("Repo '"+name+"' URI doesn't look like a URI: "+rawUri);
		this.directoryizedUri = UriUtil.directoryizeUri(rawUri);
	}
	
	public static final class RepoConfigParseResult {
		public final RepoConfig repoConfig;
		public final String parseError;
		public final int offset;
		
		/** No error, no repo config; try parsing something else */
		public RepoConfigParseResult(int offset) {
			this.repoConfig = null;
			this.parseError = null;
			this.offset = offset;
		}
		public RepoConfigParseResult(RepoConfig repoConfig, int offset) {
			this.repoConfig = repoConfig;
			this.parseError = null;
			this.offset = offset;
		}
		public RepoConfigParseResult(String parseError, int offset) {
			this.repoConfig = null;
			this.parseError = parseError;
			this.offset = offset;
		}
	}
	
	public static RepoConfigParseResult parse(String[] args, int offset, String baseUri) {
		if( offset >= args.length ) return new RepoConfigParseResult("No arguments to parse as repo config", offset);
		if( offset+1 >= args.length ) return new RepoConfigParseResult(args[offset]+" should be followed by a URI argument", offset+1);
		
		String arg = args[offset];
		String[] parts = arg.split(":");
		
		String disposition;
		if( "-repo".equals(parts[0]) ) {
			disposition = DISPOSITION_DEFAULT;
		} else if( "-remote-repo".equals(parts[0]) ) {
			disposition = DISPOSITION_REMOTE;
		} else if( "-cache-repo".equals(parts[0]) ) {
			disposition = DISPOSITION_CACHE;
		} else if( "-local-repo".equals(parts[0]) ) {
			disposition = DISPOSITION_LOCAL;
		} else {
			return new RepoConfigParseResult(offset);
		}
		
		String uri = PathUtil.maybeNormalizeFileUri(PathUtil.appendPath(baseUri, args[offset+1]));
		
		return new RepoConfigParseResult(
			new RepoConfig(disposition, parts.length >= 2 ? parts[1] : null, uri),
			offset + 2
		);
	}
}