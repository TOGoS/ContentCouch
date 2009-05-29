/**
 * 
 */
package contentcouch.repository;

public class RepoConfig {
	public static final String DISPOSITION_DEFAULT = "default";
	public static final String DISPOSITION_LOCAL = "local";
	public static final String DISPOSITION_CACHE = "cache";
	public static final String DISPOSITION_REMOTE = "remote";
	
	public String disposition = DISPOSITION_DEFAULT;
	public String name = "junky-unnamed-repository";
	public String uri = "file:junk-repository/";
	public String userStoreSector   = "user";
	public String remoteCacheSector = "remote";
	public String activeCacheSector = "active";
	public ContentAddressingScheme dataScheme = Sha1Scheme.getInstance();
	
	public static RepoConfig parse(String arg) {
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
			return null;
		}
		
		RepoConfig rp = new RepoConfig();
		rp.disposition = disposition;
		if( parts.length >= 2 ) {
			rp.name = parts[1];
		} else {
			rp.name = null;
		}
		return rp;
	}
}