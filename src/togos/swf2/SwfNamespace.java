package togos.swf2;

public class SwfNamespace {
	public static final String SWF2_NS = "http://ns.nuke24.net/SWF2/";

	public static final String HTTP_SERVLET_REQUEST = SWF2_NS + "/Request/httpServletRequest";
	public static final String HTTP_SERVLET_RESPONSE = SWF2_NS + "/Request/httpServletResponse";
	public static final String COMPONENTS = SWF2_NS + "/Request/components";

	public static final String COMPONENT_CLASS = SWF2_NS + "/componentClass";
	
	//// Context vars ///
	
	public static final String CTX_NS = SWF2_NS + "Context/";
	public static final String CTX_GETTER = CTX_NS + "getter";
	public static final String CTX_CONFIG = CTX_NS + "config";
	
	//// Config vars ////
	
	public static final String CFG_NS = SWF2_NS + "Config/";
	public static final String CFG_EXTAPPS = CFG_NS + "ExternalApplications/";

	public static final String SERVLET_PATH_URI_PREFIX = "x-servlet-path:";

}
