/**
 * 
 */
package contentcouch.rdf;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import contentcouch.date.DateUtil;
import contentcouch.digest.DigestUtil;
import contentcouch.misc.Function1;
import contentcouch.value.Blob;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class RdfDirectory extends RdfNode implements Directory {
	public static Function1 DEFAULT_DIRECTORY_ENTRY_TARGET_RDFIFIER = new Function1() {
		public Object apply(Object input) {
			if( input instanceof Ref || input instanceof RdfNode ) {
				return input;
			} else if( input instanceof Directory ) {
				return new RdfDirectory( (Directory)input, this );
			} else if( input instanceof Blob ) {
				return new Ref(DigestUtil.getSha1Urn((Blob)input));
			} else {
				throw new RuntimeException("Don't know how to rdf-ify " + input.getClass().getName() );
			}
		}
	};
	
	public static class Entry extends RdfNode implements Directory.Entry {
		public Entry( Directory.Entry de, Function1 targetRdfifier ) {
			this();
			if( RdfNamespace.OBJECT_TYPE_BLOB.equals(de.getTargetType()) ) {
			} else if( RdfNamespace.OBJECT_TYPE_DIRECTORY.equals(de.getTargetType()) ) {
			} else {
				throw new RuntimeException("Don't know how to rdf-ify directory entry with target type = '" + de.getTargetType() + "'"); 
			}

			add(RdfNamespace.CCOUCH_NAME, de.getKey());
			add(RdfNamespace.CCOUCH_TARGETTYPE, de.getTargetType());

			long modified = de.getLastModified();
			if( modified != -1 ) add(RdfNamespace.DC_MODIFIED, DateUtil.formatDate(new Date(modified)));
			
			long size = de.getSize();
			if( size != -1 ) add(RdfNamespace.CCOUCH_SIZE, String.valueOf(size) );

			add(RdfNamespace.CCOUCH_TARGET, targetRdfifier.apply(de.getValue()));
		}

		public Entry() {
			super(RdfNamespace.CCOUCH_DIRECTORYENTRY);
		}
		
		public Object getValue() {
			return getSingle(RdfNamespace.CCOUCH_TARGET);
		}

		public String getTargetType() {
			return (String)getSingle(RdfNamespace.CCOUCH_TARGETTYPE);
		}

		public String getKey() {
			return (String)getSingle(RdfNamespace.CCOUCH_NAME);
		}

		public long getSize() {
			String lm = (String)getSingle(RdfNamespace.CCOUCH_SIZE);
			if( lm == null ) return -1;
			return Long.parseLong(lm);
		}

		public long getLastModified() {
			try {
				String lm = (String)this.getSingle(RdfNamespace.DC_MODIFIED);
				if( lm == null ) return -1;
				return DateUtil.parseDate(lm).getTime();
			} catch( ParseException e ) {
				System.err.println("Error parsing modified date in " + this.sourceUri);
				return -1;
			}
		}
	}
	
	public RdfDirectory() {
		super(RdfNamespace.CCOUCH_DIRECTORY);
	}
	
	public RdfDirectory( Directory dir, Function1 targetRdfifier ) {
		this();
		List entries = new ArrayList(dir.entrySet());
		Collections.sort( entries, new Comparator() {
			public int compare( Object o1, Object o2 ) {
				return ((Directory.Entry)o1).getKey().compareTo(((Directory.Entry)o2).getKey());
			}
		});
		List rdfEntries = new ArrayList();
		for( Iterator i = entries.iterator(); i.hasNext(); ) {
			Directory.Entry entry = (Directory.Entry)i.next();
			rdfEntries.add( new RdfDirectory.Entry(entry, targetRdfifier) );
		}
		add(RdfNamespace.CCOUCH_ENTRIES, rdfEntries);
	}
	
	public RdfDirectory( Directory dir ) {
		this( dir, DEFAULT_DIRECTORY_ENTRY_TARGET_RDFIFIER );
	}

	public Set entrySet() {
		List entryList = (List)this.getSingle(RdfNamespace.CCOUCH_ENTRIES);
		HashSet entries = new HashSet();
		for( Iterator i=entryList.iterator(); i.hasNext(); ) {
			RdfDirectory.Entry e = (RdfDirectory.Entry)i.next();
			entries.add(e);
		}
		return entries;
	}
	
	public Directory.Entry getEntry(String key) {
		List entryList = (List)this.getSingle(RdfNamespace.CCOUCH_ENTRIES);
		for( Iterator i=entryList.iterator(); i.hasNext(); ) {
			RdfDirectory.Entry e = (RdfDirectory.Entry)i.next();
			if( e.getKey() == key ) {
				return e;
			}
		}
		return null;
	}
}