package contentcouch.active;

import java.util.HashMap;

public class Context extends HashMap {
	public static final String URI_RESOLVER_VARNAME = "ccouch:uri-resolver";

	public static ThreadLocal instanceVar = new ThreadLocal() {
		protected Object initialValue() {
			return new Context();
		}
	};
	public static Context getInstance() {
		return (Context)instanceVar.get();
	}
}
