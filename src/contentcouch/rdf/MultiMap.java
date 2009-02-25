/**
 * 
 */
package contentcouch.rdf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MultiMap extends HashMap {
	public MultiMap() {
		super();
	}

	public MultiMap(MultiMap n) {
		super(n);
	}
	
	public void add(Object key, Object value) {
		Set i = (Set)get(key);
		if( i == null ) {
			put(key, i = new HashSet());
		}
		i.add(value);
	}
	
	public Set getSet(Object key) {
		return (Set)get(key);
	}
	
	public Object getSingle(Object key) {
		Set i = (Set)get(key);
		if( i == null ) return null;
		for( Iterator ii=i.iterator(); ii.hasNext(); ) return ii.next();
		return null;
	}

	public void importValues(Map properties) {
		for( Iterator i = properties.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry entry = (Map.Entry)i.next();
			add(entry.getKey(), entry.getValue());
		}
	}
}