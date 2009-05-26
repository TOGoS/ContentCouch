package contentcouch.active;

import java.util.HashMap;

public class Context extends HashMap {
	public static final String GENERIC_GETTER_VAR = "ccouch:getter";
	public static final String IDENTIFIER_VAR = "ccouch:identifier";

	public static ThreadLocal instanceVar = new ThreadLocal() {
		protected Object initialValue() {
			return new Context();
		}
	};
	public static Context getInstance() {
		return (Context)instanceVar.get();
	}
}
