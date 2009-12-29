package togos.swf2;

public class SwfNamespace {
	public static final String SWF2_NS = "http://ns.nuke24.net/SWF2/";

	public static final String COMPONENT_CLASS = SWF2_NS + "/componentClass";
	
	//// Context vars ///
	
	public static final String CTX_NS = SWF2_NS + "Context/";
	public static final String CTX_GETTER = CTX_NS + "getter";
	public static final String CTX_CONFIG = CTX_NS + "config";
	public static final String HTTP_SERVLET_REQUEST = CTX_NS + "httpServletRequest";
	public static final String HTTP_SERVLET_RESPONSE = CTX_NS + "httpServletResponse";
	/** All available components */
	public static final String COMPONENTS = CTX_NS + "components";
	/** The component currently handling a request */
	public static final String COMPONENT = CTX_NS + "component";
	/** The front request handler */
	public static final String FRONT = CTX_NS + "front";
	
	//// Response vars ////
	
	public static final String RES = SWF2_NS + "Res/";
	public static final String RES_HTTP_EQUIV = SWF2_NS + "HTTP/";
	
	//// Config vars ////
	
	public static final String CFG_NS = SWF2_NS + "Config/";
	public static final String CFG_EXTAPPS = CFG_NS + "ExternalApplications/";

	public static final String SERVLET_PATH_URI_PREFIX = "x-servlet-path:";

}
