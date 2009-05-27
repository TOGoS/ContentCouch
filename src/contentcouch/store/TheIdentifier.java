package contentcouch.store;

import contentcouch.active.Context;

public class TheIdentifier {
	public static Identifier globalInstance;
	
	public static Identifier getGenericIdentifier() {
		Identifier theIdentifier = (Identifier)Context.getInstance().get(Context.IDENTIFIER_VAR);
		if( theIdentifier == null ) {
			theIdentifier = globalInstance;
		}
		if( theIdentifier == null ) {
			throw new RuntimeException("No "+Context.IDENTIFIER_VAR+" registered");
		}
		return theIdentifier;
	}
	
	public static String identify(Object o) {
		return getGenericIdentifier().identify(o);
	}
	
	public static String identifyAt(String uri) {
		return getGenericIdentifier().identifyAt(uri);
	}
}
