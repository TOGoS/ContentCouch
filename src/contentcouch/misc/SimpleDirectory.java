package contentcouch.misc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import contentcouch.rdf.RdfNamespace;
import contentcouch.value.Directory;
import contentcouch.value.MetadataHaver;

public class SimpleDirectory implements Directory, MetadataHaver {
	public static class Entry implements Directory.Entry, MetadataHaver {
		public long lastModified;
		public String name;
		public long size;
		public Object target;
		public String targetType;
		public Map metadata;

		public long getLastModified() { return lastModified; }
		public String getName() { return name; }
		public long getSize() { return size; }
		public Object getTarget() { return target; }
		public String getTargetType() { return targetType; }
		
		public Map getMetadata() { return metadata; }
		
		public void putMetadata(String key, Object value) {
			if( metadata == null ) metadata = new HashMap();
			metadata.put(key,value);
		}
	}
	
	public Map entries = new HashMap();
	public Map metadata;
	
	public SimpleDirectory(Map m) {
		for( Iterator i=m.entrySet().iterator(); i.hasNext(); ) {
			final Map.Entry e = (Map.Entry)i.next();
			SimpleDirectory.Entry sde = new SimpleDirectory.Entry();
			sde.name = (String)e.getKey();
			sde.lastModified = -1;
			sde.target = e.getValue();
			sde.size = -1;
			sde.targetType = RdfNamespace.OBJECT_TYPE_DIRECTORY;
			addEntry(sde);
		}
	}
	
	public Map getEntries() { return entries; }
	
	public void addEntry(Directory.Entry e) { entries.put(e.getName(), e); }
	
	//// MetadataHaver implementation ////
	
	public Map getMetadata() { return metadata; }
	
	public void putMetadata(String key, Object value) {
		if( metadata == null ) metadata = new HashMap();
		metadata.put(key,value);
	}
}
