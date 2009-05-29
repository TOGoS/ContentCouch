package contentcouch.misc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import contentcouch.rdf.CcouchNamespace;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class SimpleDirectory implements Directory {
	public static class Entry implements Directory.Entry {
		public long lastModified = -1;
		public String name;
		public long size = -1;
		public Object target;
		public String targetType;
		public Map metadata;

		public long getLastModified() { return lastModified; }
		public String getKey() { return name; }
		public long getSize() { return size; }
		public Object getValue() { return target; }
		public String getTargetType() { return targetType; }
		
		public Map getMetadata() { return metadata; }
		
		public Object getMetadata(String key) {
			if( metadata == null ) return null;
			return metadata.get(key);
		}
		
		public void putMetadata(String key, Object value) {
			if( metadata == null ) metadata = new HashMap();
			metadata.put(key,value);
		}
	}

	public static final int DEEPCLONE_NEVER = 0;
	public static final int DEEPCLONE_SIMPLEDIRECTORY = 1;
	public static final int DEEPCLONE_ALWAYS = 2;
	
	public static Object cloneTarget( Object target, int depth ) {
		if( depth == DEEPCLONE_NEVER ) return target;
		if( target instanceof Ref ) {
			target = TheGetter.get( ((Ref)target).targetUri );
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
		destEntry.name = srcEntry.getKey();
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
	
	public Set getDirectoryEntrySet() {
		return new HashSet(entryMap.values());
	}
	
	public Directory.Entry getDirectoryEntry(String name) {
		return (Directory.Entry)entryMap.get(name);
	}
	
	public void addEntry(Directory.Entry e) {
		entryMap.put(e.getKey(), e);
	}
}
