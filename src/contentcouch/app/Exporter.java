package contentcouch.app;

import java.io.File;
import java.util.Date;
import java.util.Iterator;

import contentcouch.app.Linker.LinkException;
import contentcouch.blob.BlobUtil;
import contentcouch.file.FileBlob;
import contentcouch.misc.MetadataUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.RdfNamespace;
import contentcouch.rdf.RdfNode;
import contentcouch.store.Getter;
import contentcouch.store.Identifier;
import contentcouch.store.ParseRdfGetFilter;
import contentcouch.value.Blob;
import contentcouch.value.Commit;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class Exporter {
	Getter getter;
	Identifier identifier;
	public boolean link = false;
	public boolean exportFiles = true; 
	public boolean replaceFiles = false;
	
	public static interface MergeConflictHandler {
		/** Return true if checkout should continue, false otherwise. */
		public boolean handleMergeConflict( String path, String localUrn, Object localObj, String remoteUrn, Object remoteObj );
	}
	
	public MergeConflictHandler mergeConflictHandler = new MergeConflictHandler() {
		public boolean handleMergeConflict(String path, String localUrn, Object localObj, String remoteUrn, Object remoteObj) {
			throw new RuntimeException("File already exists and is different from blob being exported: " + path + " != " + remoteUrn);			
		}
	};
	
	public Exporter( Getter getter, Identifier identifier ) {
		if( !(getter instanceof ParseRdfGetFilter) ) {
			getter = new ParseRdfGetFilter(getter);
			((ParseRdfGetFilter)getter).handleAtSignAsParseRdf = true;
		}
		this.getter = getter;
		this.identifier = identifier;
	}

	//// Export functions ////

	public void exportBlob( Blob blob, File destination, String blobSourceUri ) {
		boolean fileMade = false;
		
		if( destination.exists() && !replaceFiles ) {
			if( identifier == null ) {
				throw new RuntimeException("Exporter needs to identify some files but #identifier is not set up");
			}
			FileBlob fileBlob = new FileBlob(destination);
			String fileUrn = identifier.identify(fileBlob);
			String newUrn = identifier.identify(blob);
			if( fileUrn.equals(newUrn) ) {
				Log.log(Log.LEVEL_CHATTY, Log.TYPE_UNCHANGED, destination.getPath() + " = " + blobSourceUri);
				return;
			} else {
				if( !mergeConflictHandler.handleMergeConflict(destination.getPath(), fileUrn, fileBlob, newUrn, blob) ) {
					return;
				}
			}
		}
		
		Log.log(Log.LEVEL_CHANGES, Log.TYPE_EXPORTING, destination.getPath() + " from " + blobSourceUri);
		
		if( link && blob instanceof File ) {
			try {
				Linker.getInstance().relink((File)blob, destination);
				fileMade = true;
			} catch( LinkException e ) {
				Log.log(Log.LEVEL_WARNINGS, Log.TYPE_WARNING, "Failed to hardlink " + destination + " to " + (File)blob + "; will copy");
			}
		}
		if( !fileMade ) {
			if( destination.exists() ) {
				if( !destination.delete() ) {
					throw new RuntimeException("Could not delete existing file at " + destination.getPath());
				}
			}
			BlobUtil.writeBlobToFile(blob, destination);
		}
		Date lm = (Date)MetadataUtil.getMetadataFrom(blob, RdfNamespace.DC_MODIFIED);
		if( lm != null ) destination.setLastModified(lm.getTime());
	}
	
	protected void exportDirectoryEntry( Directory.Entry entry, File destDir, String entrySourceLocation ) {
		String name = entry.getName();
		if( name.endsWith("/") ) name = name.substring(0, name.length()-1);
		if( (name.indexOf('/') != -1) || (name.indexOf('\\') != -1) ) {
			throw new RuntimeException("Invalid characters in directory entry name: " + name);
		}
		
		File destination = new File(destDir + "/" + name);
		
		Object target = entry.getTarget();
		if( target == null ) {
			throw new RuntimeException( "Entry has no target: " + entrySourceLocation );
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
		Log.log(Log.LEVEL_CHATTY, "Merging " + destination.getPath() + " from " + directorySourceLocation);
		if( !destination.exists() ) {
			destination.mkdirs();
		}
		for( Iterator i = dir.getEntries().values().iterator(); i.hasNext(); ) {
			exportDirectoryEntry( (Directory.Entry)i.next(), destination, directorySourceLocation );
		}
	}
	
	public Object followRedirects( Object obj, String referencedBy ) {
		while( true ) {
			if( obj instanceof Ref ) {
				String uri = ((Ref)obj).targetUri;
				obj = getter.get(uri);
				if( obj == null ) throw new RuntimeException("Couldn't find " + uri + (referencedBy == null ? "" : ", referenced by " + referencedBy ));
			} else if( obj instanceof RdfNode && RdfNamespace.CCOUCH_REDIRECT.equals(((RdfNode)obj).typeName) ) {
				obj = ((RdfNode)obj).getSingle(RdfNamespace.CCOUCH_TARGET);
			} else {
				return obj;
			}
		}
	}
	
	public void exportObject( Object obj, File destination, String referencedBy ) {
		if( obj instanceof Ref ) {
			String uri = PathUtil.appendPath(referencedBy, ((Ref)obj).targetUri);
			obj = getter.get(uri);
			if( obj == null ) throw new RuntimeException("Couldn't find " + uri + (referencedBy == null ? "" : ", referenced by " + referencedBy ));
			exportObject( obj, destination, uri );
		} else if( obj instanceof RdfNode && RdfNamespace.CCOUCH_REDIRECT.equals(((RdfNode)obj).typeName) ) {
			obj = ((RdfNode)obj).getSingle(RdfNamespace.CCOUCH_TARGET);
			exportObject( obj, destination, referencedBy );
		} else if( obj instanceof Blob ) {
			exportBlob( (Blob)obj, destination, referencedBy );
		} else if( obj instanceof Directory ) {
			exportDirectory( (Directory)obj, destination, referencedBy );
		} else if( obj instanceof Commit ) {
			Object target = ((Commit)obj).getTarget();
			exportObject( target, destination, ((Commit)obj).getUri() );
		} else {
			throw new RuntimeException("Don't know how to export " + obj.getClass().getName() );
		}
	}
	
	public void exportObject( Object obj, File destination ) {
		exportObject( obj, destination, null );
	}
}
