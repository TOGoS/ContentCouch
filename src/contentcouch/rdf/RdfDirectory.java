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
import contentcouch.value.BaseRef;
import contentcouch.value.Blob;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class RdfDirectory extends RdfNode implements Directory {
	/** Turns Blobs into Refs, keeps Refs and RdfNodes as they are, and turns Directories into nested Rdf */
	public static Function1 DEFAULT_TARGET_RDFIFIER = new Function1() {
		public Object apply(Object input) {
			if( input == null ) {
				return null;
			} else if( input instanceof Ref || input instanceof RdfNode ) {
				return input;
			} else if( input instanceof Directory ) {
				return new RdfDirectory( (Directory)input, this );
			} else if( input instanceof Blob ) {
				return new BaseRef(DigestUtil.getSha1Urn((Blob)input));
			} else {
				throw new RuntimeException("Don't know how to rdf-ify " + input.getClass().getName() );
			}
		}
	};
	
	public static class Entry extends RdfNode implements Directory.Entry {
		public Entry() {
			super(CcouchNamespace.DIRECTORYENTRY);
		}

		public Entry( Directory.Entry de, Function1 targetRdfifier ) {
			this();
			if( de.getTargetType() == null ) {
			} else if( CcouchNamespace.OBJECT_TYPE_BLOB.equals(de.getTargetType()) ) {
			} else if( CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(de.getTargetType()) ) {
			} else {
				throw new RuntimeException("Don't know how to rdf-ify directory entry with target type = '" + de.getTargetType() + "'"); 
			}

			add(CcouchNamespace.NAME, de.getName());
			add(CcouchNamespace.TARGETTYPE, de.getTargetType());

			long modified = de.getTargetLastModified();
			if( modified != -1 ) add(DcNamespace.DC_MODIFIED, DateUtil.formatDate(new Date(modified)));
			
			long size = de.getTargetSize();
			if( size != -1 ) add(CcouchNamespace.SIZE, String.valueOf(size) );

			add(CcouchNamespace.TARGET, targetRdfifier.apply(de.getTarget()));
		}
		
		public Entry( Directory.Entry de, Ref target ) {
			this();
			if( de.getTargetType() == null ) {
			} else if( CcouchNamespace.OBJECT_TYPE_BLOB.equals(de.getTargetType()) ) {
			} else if( CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(de.getTargetType()) ) {
			} else {
				throw new RuntimeException("Don't know how to rdf-ify directory entry with target type = '" + de.getTargetType() + "'"); 
			}

			add(CcouchNamespace.NAME, de.getName());
			add(CcouchNamespace.TARGETTYPE, de.getTargetType());

			long modified = de.getTargetLastModified();
			if( modified != -1 ) add(DcNamespace.DC_MODIFIED, DateUtil.formatDate(new Date(modified)));
			
			long size = de.getTargetSize();
			if( size != -1 ) add(CcouchNamespace.SIZE, String.valueOf(size) );

			add(CcouchNamespace.TARGET, target);
		}
		
		public Object getTarget() {
			return getSingle(CcouchNamespace.TARGET);
		}

		public String getTargetType() {
			return (String)getSingle(CcouchNamespace.TARGETTYPE);
		}

		public String getName() {
			return (String)getSingle(CcouchNamespace.NAME);
		}

		public long getTargetSize() {
			String lm = (String)getSingle(CcouchNamespace.SIZE);
			if( lm == null ) return -1;
			return Long.parseLong(lm);
		}

		public long getTargetLastModified() {
			try {
				String lm = (String)this.getSingle(DcNamespace.DC_MODIFIED);
				if( lm == null ) return -1;
				return DateUtil.parseDate(lm).getTime();
			} catch( ParseException e ) {
				System.err.println("Error parsing modified date in " + this.sourceUri);
				return -1;
			}
		}
	}
	
	public RdfDirectory() {
		super(CcouchNamespace.DIRECTORY);
	}
	
	public RdfDirectory( Directory dir, Function1 targetRdfifier ) {
		this();
		if( targetRdfifier == null ) targetRdfifier = DEFAULT_TARGET_RDFIFIER;
		List entries = new ArrayList(dir.getDirectoryEntrySet());
		Collections.sort( entries, new Comparator() {
			public int compare( Object o1, Object o2 ) {
				return ((Directory.Entry)o1).getName().compareTo(((Directory.Entry)o2).getName());
			}
		});
		List rdfEntries = new ArrayList();
		for( Iterator i = entries.iterator(); i.hasNext(); ) {
			Directory.Entry entry = (Directory.Entry)i.next();
			rdfEntries.add( new RdfDirectory.Entry(entry, targetRdfifier) );
		}
		add(CcouchNamespace.ENTRIES, rdfEntries);
	}
	
	public RdfDirectory( Directory dir ) {
		this( dir, null );
	}

	public Set getDirectoryEntrySet() {
		List entryList = (List)this.getSingle(CcouchNamespace.ENTRIES);
		HashSet entries = new HashSet();
		if( entryList != null )	for( Iterator i=entryList.iterator(); i.hasNext(); ) {
			RdfDirectory.Entry e = (RdfDirectory.Entry)i.next();
			entries.add(e);
		}
		return entries;
	}
	
	public Directory.Entry getDirectoryEntry(String key) {
		List entryList = (List)this.getSingle(CcouchNamespace.ENTRIES);
		for( Iterator i=entryList.iterator(); i.hasNext(); ) {
			RdfDirectory.Entry e = (RdfDirectory.Entry)i.next();
			if( e.getName() == key ) {
				return e;
			}
		}
		return null;
	}
	
	public void addDirectoryEntry( Directory.Entry newEntry ) {
		List entryList = (List)this.getSingle(CcouchNamespace.ENTRIES);
		if( entryList == null ) {
			entryList = new ArrayList();
			this.add(CcouchNamespace.ENTRIES, entryList);
		}
		if( !(newEntry instanceof RdfDirectory.Entry) ) {
			throw new RuntimeException( "Can only add RdfDirectory.Entries to RdfDirectory");
		}
		entryList.add(newEntry);
	}
}