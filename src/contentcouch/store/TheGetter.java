package contentcouch.store;

import contentcouch.active.Context;

public class TheGetter {
	public static Getter globalInstance;
	
	public static Getter getGenericGetter() {
		Getter theGetter = (Getter)Context.getInstance().get(Context.GENERIC_GETTER_VAR);
		if( theGetter == null ) {
			theGetter = globalInstance;
		}
		if( theGetter == null ) {
			throw new RuntimeException("No "+Context.GENERIC_GETTER_VAR+" registered");
		}
		return theGetter;
	}
	
	public static Object get(String uri) {
		return getGenericGetter().get(uri);
	}
}
