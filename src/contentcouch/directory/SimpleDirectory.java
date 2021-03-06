package contentcouch.directory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import contentcouch.framework.TheGetter;
import contentcouch.misc.Function1;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class SimpleDirectory implements WritableDirectory, Map {
	static class SimpleMapEntry implements Map.Entry {
		Object k, v;
		public SimpleMapEntry( Object k, Object v ) {
			this.k = k; this.v = v;
		}
		public Object getKey() { return k; }
		public Object getValue() { return v; }
		public Object setValue(Object v) {
			Object oldV = this.v;
			this.v = v;
			return oldV;
		}
	}
	
	public static class Entry implements Directory.Entry, Map.Entry {
		public long lastModified = -1;
		public String name;
		public long targetSize = -1;
		public Object target;
		public String targetType;
		
		public Entry() { }
		
		public Entry(Directory.Entry e) {
			this.name = e.getName();
			this.target = e.getTarget();
			this.targetType = e.getTargetType();
			this.targetSize = e.getTargetSize();
			this.lastModified = e.getLastModified();
		}
		
		public Entry( String name, Object target, String targetType ) {
			this.name = name;
			this.target = target;
			this.targetType = targetType;
		}
		
		public Entry( String name, Object target, String targetType, long lastModified ) {
			this.name = name;
			this.target = target;
			this.targetType = targetType;
			this.lastModified = lastModified;
		}
		
		//// Directory.Entry implementation ////
		public long getLastModified() { return lastModified; }
		public String getName() { return name; }
		public long getTargetSize() { return targetSize; }
		public Object getTarget() { return target; }
		public String getTargetType() { return targetType; }
		
		//// Map.Entry implementation ////
		public Object getKey() { return name; }
		public Object getValue() {
			if( target instanceof Ref ) {
				return TheGetter.get(((Ref)target).getTargetUri());
			} else {
				return target;
			}
		}
		public Object setValue( Object value ) {
			Object oldValue = this.target;
			this.target = value;
			return oldValue;
		}
	}

	public Map entryMap = new HashMap();
	public Map metadata;
	
	public SimpleDirectory() {
	}
	
	public SimpleDirectory( SimpleDirectory sd ) {
		this.entryMap = new HashMap(sd.entryMap);
	}
	
	public SimpleDirectory( Directory d, Function1 directoryTargetProcessor, Function1 blobTargetProcessor ) {
		for( Iterator i=d.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry entry = (Directory.Entry)i.next();
			Object target;
			if( CCouchNamespace.TT_SHORTHAND_DIRECTORY.equals(entry.getTargetType()) ) {
				target = directoryTargetProcessor.apply(entry.getTarget());
			} else {
				target = blobTargetProcessor.apply(entry.getTarget());
			}
			if( target == null ) continue;
			Entry newEntry = new Entry();
			newEntry.name = entry.getName();
			newEntry.target = target;
			newEntry.targetType = entry.getTargetType();
			newEntry.targetSize = entry.getTargetSize();
			newEntry.lastModified = entry.getLastModified();
			addDirectoryEntry(newEntry, Collections.EMPTY_MAP);
		}
	}
	
	public SimpleDirectory(Map m) {
		for( Iterator i=m.entrySet().iterator(); i.hasNext(); ) {
			final Map.Entry e = (Map.Entry)i.next();
			SimpleDirectory.Entry sde = new SimpleDirectory.Entry();
			sde.name = (String)e.getKey();
			sde.lastModified = -1;
			sde.target = e.getValue();
			sde.targetSize = -1;
			sde.targetType = CCouchNamespace.TT_SHORTHAND_DIRECTORY;
			addDirectoryEntry(sde, Collections.EMPTY_MAP);
		}
	}
	
	//// Directory implementation ////
	
	public Set getDirectoryEntrySet() {
		return new HashSet(entryMap.values());
	}
	
	public Directory.Entry getDirectoryEntry(String name) {
		return (Directory.Entry)entryMap.get(name);
	}
	
	public void addDirectoryEntry(Directory.Entry e, Map options) {
		entryMap.put(e.getName(), new Entry(e));
	}
	
	public void addDirectoryEntry(Directory.Entry e) {
		addDirectoryEntry(e, Collections.EMPTY_MAP);
	}
	
	public void deleteDirectoryEntry(String name, Map options) {
		this.entryMap.remove(name);
	}
	
	////
	
	protected Entry createEntry( String name, Object value ) {
		Entry newEntry = new Entry();
		newEntry.target = value;
		newEntry.name = name;
		if( value instanceof Directory ) {
			newEntry.targetType = CCouchNamespace.TT_SHORTHAND_DIRECTORY;
		} else {
			newEntry.targetType = CCouchNamespace.TT_SHORTHAND_BLOB;
		}
		return newEntry;
	}

	//// Map implementation ////

	public void clear() {
		entryMap.clear();
	}
	
	public boolean containsKey(Object key) {
		return entryMap.containsKey(key);
	}
	
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}
	
	public Object get(Object key) {
		Directory.Entry e = getDirectoryEntry(key.toString());
		return (e == null) ? null : DirectoryUtil.resolveTarget(e);
	}

	public boolean isEmpty() {
		return entryMap.isEmpty();
	}

	public Object put(Object key, Object value) {
		Object oldValue = get(key);
		entryMap.put(key, createEntry(ValueUtil.getString(key), value));
		return oldValue;
	}

	public void putAll(Map newStuff) {
		if( newStuff instanceof Directory ) {
			for( Iterator i=((Directory)newStuff).getDirectoryEntrySet().iterator(); i.hasNext(); ) {
				Directory.Entry e = (Directory.Entry)i.next();
				entryMap.put( e.getName(), new Entry(e) );
			}
		} else {
			for( Iterator i=newStuff.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry me = (Map.Entry)i.next();
				Entry newEntry;
				if( me instanceof Directory.Entry ) {
					newEntry = new Entry((Directory.Entry)me);
				} else {
					newEntry = createEntry(ValueUtil.getString(me.getKey()), me.getValue());
				}
				entryMap.put( newEntry.getName(), newEntry );
			}
		}
	}

	public Object remove(Object key) {
		Entry e = (Entry)entryMap.remove(key);
		if( e == null ) return null;
		return e.getTarget();
	}

	public int size() {
		return entryMap.size();
	}

	public Collection values() {
		ArrayList values = new ArrayList();
		for( Iterator i=entryMap.values().iterator(); i.hasNext(); ) {
			values.add( DirectoryUtil.resolveTarget((Entry)i.next()) );
		}
		return values;
	}

	public Set entrySet() {
		return new HashSet(entryMap.values());
	}

	public Set keySet() {
		return entryMap.keySet();
	}
}
