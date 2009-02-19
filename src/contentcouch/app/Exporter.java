package contentcouch.app;

import java.io.File;
import java.util.Date;
import java.util.Iterator;

import contentcouch.app.Linker.LinkException;
import contentcouch.blob.BlobUtil;
import contentcouch.misc.MetadataUtil;
import contentcouch.rdf.RdfNamespace;
import contentcouch.rdf.RdfNode;
import contentcouch.store.Getter;
import contentcouch.store.ParseRdfGetFilter;
import contentcouch.value.Blob;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class Exporter {
	Getter getter;
	public boolean verbose = false;
	public boolean link = false;
	public boolean exportFiles = true; 
	
	public Exporter( Getter getter ) {
		if( !(getter instanceof ParseRdfGetFilter) ) {
			getter = new ParseRdfGetFilter(getter);
			((ParseRdfGetFilter)getter).handleAtSignAsParseRdf = true;
		}
		this.getter = getter;
	}

	//// Export functions ////

	public void exportBlob( Blob blob, File destination ) {
		if( verbose ) System.err.println(destination.getPath());
		boolean fileMade = false;
		if( link && blob instanceof File ) {
			try {
				Linker.getInstance().link((File)blob, destination);
				fileMade = true;
			} catch( LinkException e ) {
				System.err.println("Failed to hardlink " + destination + " to " + (File)blob + "; will copy");
			}
		}
		if( !fileMade ) {
			BlobUtil.writeBlobToFile(blob, destination);
		}
		Date lm = (Date)MetadataUtil.getMetadataFrom(blob, RdfNamespace.DC_MODIFIED);
		if( lm != null ) destination.setLastModified(lm.getTime());
	}
	
	protected void exportDirectoryEntry( Directory.Entry entry, File destDir, String entrySourceLocation ) {
		String name = entry.getName();
		if( (name.indexOf('/') != -1) || (name.indexOf('\\') != -1) ) {
			throw new RuntimeException("Invalid characters in directory entry name: " + name);
		}
		
		File destination = new File(destDir + "/" + name);
		
		Object target = entry.getTarget();
		if( target == null ) {
			throw new RuntimeException("Entry targeted nothing " + entry);
		}
	
		if( RdfNamespace.OBJECT_TYPE_BLOB.equals(entry.getTargetType()) ) {
			if( exportFiles ) {
				exportObject( target, destination, entrySourceLocation );
				long mtime = entry.getLastModified();
				if( mtime != -1 ) {
					destination.setLastModified( mtime );
				}
				
				// If we wanted, we could check here that the size matched what the entry said it was
			}
		} else {
			exportObject( target, destination, entrySourceLocation );
		}
	}

	public void exportDirectory( Directory dir, File destination, String directorySourceLocation ) {
		if( verbose ) System.err.println(destination.getPath());
		if( !destination.exists() ) {
			destination.mkdirs();
		}
		for( Iterator i = dir.getEntries().values().iterator(); i.hasNext(); ) {
			exportDirectoryEntry( (Directory.Entry)i.next(), destination, directorySourceLocation );
		}
	}
	
	public void exportObject( Object obj, File destination, String referencedBy ) {
		if( obj instanceof Ref ) {
			String uri = ((Ref)obj).targetUri;
			obj = getter.get(uri);
			if( obj == null ) throw new RuntimeException("Couldn't find " + uri + (referencedBy == null ? "" : ", referenced by " + referencedBy ));
			exportObject(obj, destination, uri);
		} else if( obj instanceof Blob ) {
			exportBlob( (Blob)obj, destination );
		} else if( obj instanceof Directory ) {
			exportDirectory( (Directory)obj, destination, referencedBy );
		} else if( obj instanceof RdfNode && (RdfNamespace.CCOUCH_COMMIT.equals(((RdfNode)obj).typeName) || RdfNamespace.CCOUCH_REDIRECT.equals(((RdfNode)obj).typeName)) ) {
			Object target = ((RdfNode)obj).get(RdfNamespace.CCOUCH_TARGET);
			exportObject( target, destination, referencedBy );
		} else {
			throw new RuntimeException("Don't know how to export " + obj.getClass().getName() );
		}
	}
	
	public void exportObject( Object obj, File destination ) {
		exportObject( obj, destination, null );
	}
}
