package contentcouch.rdf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MultiMap extends HashMap
{
	private static final long serialVersionUID = 1L;

	public MultiMap() {
		super();
	}

	public MultiMap(MultiMap n) {
		for( Iterator i=n.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			put( e.getKey(), new HashSet((Set)e.getValue()) );
		}
	}
	
	public void add(Object key, Object value) {
		Set i = (Set)get(key);
		if( i == null ) {
			put(key, i = new HashSet());
		}
		i.add(value);
	}
	
	public void putSingle(Object key, Object value) {
		Set i = new HashSet();
		i.add(value);
		put(key, i);
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