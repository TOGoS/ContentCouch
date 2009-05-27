// -*- tab-width:4 -*-
package contentcouch.store;

public interface Identifier {
	public String identify( Object obj );
	public String identifyAt( String uri );
}
