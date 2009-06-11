package contentcouch.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import contentcouch.rdf.CcouchNamespace;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class SimpleDirectory implements Directory, Map {
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
		public long size = -1;
		public Object target;
		public String targetType;

		public Entry() { }
		public Entry(Directory.Entry e) {
			this.target = e.getValue();
			this.targetType = e.getTargetType();
			this.name = e.getName();
			this.size = e.getSize();
			this.lastModified = e.getLastModified();
		}
		
		//// Directory.Entry implementation ////
		public long getLastModified() { return lastModified; }
		public String getName() { return name; }
		public long getSize() { return size; }
		public Object getValue() { return target; }
		public String getTargetType() { return targetType; }

		//// Map.Entry implementation ////
		public Object getKey() { return name; }
		public Object setValue( Object value ) {
			Object oldTarget = target;
			this.target = value;
			return oldTarget;
		}
	}

	public static final int DEEPCLONE_NEVER = 0;
	public static final int DEEPCLONE_SIMPLEDIRECTORY = 1;
	public static final int DEEPCLONE_ALWAYS = 2;
	
	public static Object cloneTarget( Object target, int depth ) {
		if( depth == DEEPCLONE_NEVER ) return target;
		if( target instanceof Ref ) {
			target = TheGetter.get( ((Ref)target).getTargetUri() );
		}
		if( target instanceof Directory && depth == DEEPCLONE_ALWAYS ) {
			return new SimpleDirectory((Directory)target, depth);
		} else if( target instanceof SimpleDirectory && depth == DEEPCLONE_SIMPLEDIRECTORY ) {
			return new SimpleDirectory((Directory)target, depth);
		} else {
			return target;	
		}
	}
	
	public static void cloneMetadataInto( SimpleDirectory.Entry destEntry, Directory.Entry srcEntry ) {
		destEntry.name = srcEntry.getName();
		destEntry.targetType = srcEntry.getTargetType();
		destEntry.size = srcEntry.getSize();
		destEntry.lastModified = srcEntry.getLastModified();
	}
	
	public static void cloneInto( SimpleDirectory.Entry destEntry, Directory.Entry srcEntry, int depth ) {
		Object srcTarget = srcEntry.getValue();
		if( CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(srcEntry.getTargetType()) ) {
			boolean clone;
			switch( depth ) {
			case( DEEPCLONE_NEVER ): clone = false; break;
			case( DEEPCLONE_SIMPLEDIRECTORY ): clone = srcTarget instanceof SimpleDirectory; break;
			case( DEEPCLONE_ALWAYS ): clone = true;
			default: throw new RuntimeException("Invalid value for depth: " + depth);
			}
			destEntry.target = clone ? cloneTarget( srcTarget, depth ) : srcTarget;
		} else {
			destEntry.target = srcTarget; 
		}
		cloneMetadataInto( destEntry, srcEntry );
	}

	protected static SimpleDirectory.Entry cloneEntry( Directory.Entry e, int depth ) {
		SimpleDirectory.Entry newEntry = new SimpleDirectory.Entry();
		cloneInto( newEntry, e, depth );
		return newEntry;
	}
	
	public Map entryMap = new HashMap();
	public Map metadata;
	
	public SimpleDirectory() {
	}
	
	public SimpleDirectory( SimpleDirectory sd ) {
		this.entryMap = new HashMap(sd.entryMap);
	}
	
	public SimpleDirectory( Directory d, int depth ) {
		for( Iterator i=d.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry entry = (Directory.Entry)i.next();
			entry = cloneEntry(entry, depth);
			addEntry(entry); 
		}
	}
	
	public SimpleDirectory(Map m) {
		for( Iterator i=m.entrySet().iterator(); i.hasNext(); ) {
			final Map.Entry e = (Map.Entry)i.next();
			SimpleDirectory.Entry sde = new SimpleDirectory.Entry();
			sde.name = (String)e.getKey();
			sde.lastModified = -1;
			sde.target = e.getValue();
			sde.size = -1;
			sde.targetType = CcouchNamespace.OBJECT_TYPE_DIRECTORY;
			addEntry(sde);
		}
	}
	
	//// Directory implementation ////
	
	public Set getDirectoryEntrySet() {
		return new HashSet(entryMap.values());
	}
	
	public Directory.Entry getDirectoryEntry(String name) {
		return (Directory.Entry)entryMap.get(name);
	}
	
	public void addEntry(Directory.Entry e) {
		entryMap.put(e.getName(), new Entry(e));
	}
	
	////
	
	protected Entry createEntry( String name, Object value ) {
		Entry newEntry = new Entry();
		newEntry.target = value;
		newEntry.name = name;
		if( value instanceof Directory ) {
			newEntry.targetType = CcouchNamespace.OBJECT_TYPE_DIRECTORY;
		} else {
			newEntry.targetType = CcouchNamespace.OBJECT_TYPE_BLOB;
		}
		return newEntry;
	}

	protected Object getActualEntryValue( Directory.Entry e ) {
		Object value = e.getValue();
		if( value instanceof Ref ) {
			return TheGetter.get(((Ref)value).getTargetUri());
		} else {
			return value;
		}
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
		return (e == null) ? null : getActualEntryValue(e);
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
		return e.getValue();
	}

	public int size() {
		return entryMap.size();
	}

	public Collection values() {
		ArrayList values = new ArrayList();
		for( Iterator i=entryMap.values().iterator(); i.hasNext(); ) {
			values.add( getActualEntryValue((Entry)i.next()) );
		}
		return values;
	}

	public Set entrySet() {
		HashSet mapEntries = new HashSet();
		for( Iterator i=entryMap.values().iterator(); i.hasNext(); ) {
			Entry e = (Entry)i.next();
			mapEntries.add( new SimpleMapEntry( e.getKey(), getActualEntryValue(e) ) );
		}
		return mapEntries;
	}

	public Set keySet() {
		return entryMap.keySet();
	}
}
