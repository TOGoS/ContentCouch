package contentcouch.store;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MultiGetter implements Getter {
	protected List subGetters;
	
	public MultiGetter() {
		this.subGetters = new ArrayList();
	}
	
	public void addGetter(Getter bg) {
		if( bg != null ) this.subGetters.add(0, bg);
	}
	
	public Object get(String identifier) {
		for( Iterator i=subGetters.iterator(); i.hasNext(); ) {
			Object obj = ((Getter)i.next()).get(identifier);
			if( obj != null ) return obj;
		}
		return null;
	}
}
