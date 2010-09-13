package contentcouch.repository;

import java.util.Date;
import java.util.HashMap;

public class ShortTermCache
{
	protected class Entry {
		protected Object value;
		protected Date inserted;
		
		public Entry( Object value, Date inserted ) {
			this.value = value;
			this.inserted = inserted;
		}
		
		public Object getValue() {  return value;  }
		public Date getDateInserted() {  return inserted;  }  
	}
	
	protected long defaultExpireInterval;
	
	public ShortTermCache( long defaultExpireInterval ) {
		this.defaultExpireInterval = defaultExpireInterval;
	}
	
	/** May want to replace this with something
	 * that automatically overwrites old entries
	 * to limit memory usage */
	protected HashMap entries = new HashMap();
	
	public void put( Object key, Object value ) {
		entries.put( key, new Entry(value, new Date()) );
	}
	
	public Object get( Object key, long ifNewerThan ) {
		Entry e = (Entry)entries.get(key);
		if( e == null ) return null;
		long expires = e.getDateInserted().getTime() + ifNewerThan;
		long current = new Date().getTime();
		if( expires < current ) return null;
		return e.getValue();
	}
	
	public Object get( Object key ) {
		return get( key, defaultExpireInterval );
	}
}
