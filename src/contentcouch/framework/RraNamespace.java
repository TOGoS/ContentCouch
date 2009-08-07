package contentcouch.framework;

public class RraNamespace {
	
	//// Request metadata ////
	public static final String RRA_REQ_NS = "http://ns.nuke24.net/RRA/Request/";
	public static final String REQ_CACHEING_DESIRED = RRA_REQ_NS + "cachingDesired";
	public static final String REQ_AUTH_USERNAME = RRA_REQ_NS + "authUsername";
	public static final String REQ_AUTH_PASSWORD = RRA_REQ_NS + "authPassword";
	public static final String REQ_AUTH_PASSHASH = RRA_REQ_NS + "authPasshash";
	/** Indicates the name of the user who has been verified */
	public static final String REQ_USERNAME = RRA_REQ_NS + "username";
	
	//// Response metadata ////
	public static final String RRA_RES_NS = "http://ns.nuke24.net/RRA/Response/";
	public static final String RES_CACHEABLE = RRA_RES_NS + "cacheable";
	
}
