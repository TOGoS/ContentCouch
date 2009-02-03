package contentcouch.app;

import java.io.File;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;

import contentcouch.data.Blob;
import contentcouch.data.BlobUtil;
import contentcouch.store.ParseRdfGetFilter;
import contentcouch.store.Getter;
import contentcouch.store.Sha1BlobStore;
import contentcouch.xml.RDF;
import contentcouch.xml.RDF.RdfNode;
import contentcouch.xml.RDF.Ref;

public class Exporter {
	Getter getter;
	boolean link = false;
	boolean verbose = false;
	
	public Exporter( Getter getter ) {
		if( !(getter instanceof ParseRdfGetFilter) ) {
			getter = new ParseRdfGetFilter(getter);
		}
		this.getter = getter;
	}

	public void exportFile( Blob blob, File destination ) {
		if( verbose ) System.err.println(destination.getPath());
		BlobUtil.writeBlobToFile(blob, destination);
	}
	
	public void exportFile( String fileUri, File destination ) {
		Blob blob = BlobUtil.getBlob(getter.get(fileUri));
		if( blob == null ) {
			throw new RuntimeException("Couldn't find blob: " + fileUri);
		}
		exportFile( blob, destination );
	}
	
	protected Object getRdf( Blob blob, String sourceUri ) {
		return RDF.parseRdf(BlobUtil.getString(blob), sourceUri);
	}
	
	protected Object getRdf( Object obj, String sourceUri ) {
		if( obj instanceof Ref ) {
			String targetUri = ((Ref)obj).targetUri;
			obj = getter.get(targetUri);
			if( obj == null ) throw new RuntimeException("Could not load " + ((Ref)obj).targetUri );
			return getRdf(obj, targetUri);
		} else if( obj instanceof Blob ) {
			return getRdf((Blob)obj, sourceUri);
		} else if( obj instanceof RdfNode || obj instanceof String ) {
			return obj;
		} else if( obj == null ) {
			return null;
		} else {
			throw new RuntimeException("Could not get RdfNode from " + obj.getClass().getName());
		}
	}
		
	protected Object getTarget( RdfNode node ) {
		String targetType = (String)node.getSingle(RDF.CCOUCH_TARGETTYPE);
		Object target = node.getSingle(RDF.CCOUCH_TARGET);
		Object targetListing = node.getSingle(RDF.CCOUCH_TARGETLISTING);
		
		// Now, based on target, targetType, and targetListing, we can try to figure
		// out what this refers to.
		if( target == null && targetListing == null ) return null;
		
		if( targetType == null ) {
			if( target != null ) {
				if( target instanceof Ref ) {
					return getter.get( ((Ref)target).targetUri );
				} else if( target instanceof RdfNode ) {
					return target;
				} else {
					throw new RuntimeException(node.sourceUri + " gave a target that is a " + target.getClass().getName() + " - only Refs and RdfNodes are allowed");
				}
			} else {
				return getRdf( targetListing, node.sourceUri );
			}
		} else if( RDF.OBJECT_TYPE_BLOB.equals(targetType) ) {
			if( target != null ) {
				if( target instanceof Ref ) {
					return getter.get( ((Ref)target).targetUri );
				} else {
					return target;
				}
			} else {
				throw new RuntimeException(node.sourceUri + " gave a blob with targetListing - can only use reference blobs by target");
			}
		} else {
			if( target == null ) target = targetListing;
			return getRdf( target, node.sourceUri );
		}
	}
	
	//// Export functions (lowest-to-highest level) ////
	
	public void exportDirectoryEntry( RdfNode entry, File destDir ) {
		String name = (String)entry.getSingle(RDF.CCOUCH_NAME);
		if( (name.indexOf('/') != -1) || (name.indexOf('\\') != -1) ) throw new RuntimeException("Invalid characters in directory entry name: " + name);
		File destination = new File(destDir + "/" + name);
		
		exportObject( getTarget(entry), destination, entry );
	}
	
	public void exportObjectFromRdf( RdfNode listing, File destination ) {
		if( RDF.CCOUCH_COMMIT.equals(listing.typeName) || RDF.CCOUCH_REDIRECT.equals(listing.typeName) ) {
			Object target = getTarget( listing );
			exportObject( target, destination, listing );
		} else if( RDF.CCOUCH_DIRECTORY.equals(listing.typeName) ) {
			Collection c = (Collection)listing.getSingle(RDF.CCOUCH_ENTRIES);
			for( Iterator i=c.iterator(); i.hasNext(); ) {
				exportDirectoryEntry( (RdfNode)i.next(), destination );
			}
		} else {
			throw new RuntimeException("The RDF document at " + listing.sourceUri + " does not contain a " +
					RDF.CCOUCH_DIRECTORY + ", a " +
					RDF.CCOUCH_REDIRECT + ", or a " +
					RDF.CCOUCH_COMMIT + ", but rather a " +
					listing.typeName + ", which I don't know how to export" );
		}
	}
	
	public void exportObject( Object object, File destination, RdfNode entry ) {
		if( object instanceof Blob ) {
			exportFile( (Blob)object, destination );
			if( entry != null ) {
				String lastModifiedStr = (String)entry.getSingle(RDF.DC_MODIFIED);
				if( lastModifiedStr != null ) {
					try {
						destination.setLastModified( RDF.CCOUCH_DATEFORMAT.parse(lastModifiedStr).getTime() );
					} catch (ParseException e) {
						throw new RuntimeException("Couldn't parse date in " + entry.sourceUri + ": " + lastModifiedStr );
					}
				}
			}
		} else if( object instanceof RdfNode ) {
			exportObjectFromRdf( (RdfNode)object, destination );
		} else if( object == null ) {
			throw new RuntimeException("Can't export null" + ((entry == null) ? "" : "targetted by " + entry.sourceUri));
		} else {
			throw new RuntimeException("Don't know how to export " + object.getClass().getName() + " targeted by " + entry.sourceUri);
		}
	}
	
	public void exportObject( String uri, File destination ) {
		exportObject(getter.get(uri), destination, null);
	}
	
	public static void main(String[] args) {
		String filename = "junk-export";
		
		File file = new File(filename);
		Exporter exporter = new Exporter(new Sha1BlobStore("F:/datastore/contentcouch/data/"));
		
		exporter.exportObject("@urn:sha1:C7NV4NWLF77EQMUDYAHKKIEIB3T63WUI", file);
	}
}
