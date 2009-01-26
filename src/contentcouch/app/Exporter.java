package contentcouch.app;

import java.io.File;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;

import contentcouch.data.Blob;
import contentcouch.data.BlobUtil;
import contentcouch.store.BlobGetter;
import contentcouch.store.Sha1BlobStore;
import contentcouch.xml.RDF;
import contentcouch.xml.RDF.RdfNode;
import contentcouch.xml.RDF.Ref;

public class Exporter {
	BlobGetter blobGetter;
	
	public Exporter( BlobGetter blobSource ) {
		this.blobGetter = blobSource;
	}

	public void exportFile( String fileUri, File destination ) {
		Blob blob = blobGetter.get(fileUri);
		if( blob == null ) {
			throw new RuntimeException("Couldn't find blob: " + fileUri);
		}
		BlobUtil.writeBlobToFile(blob, destination);
	}
	
	protected Object getRdf( Object obj ) {
		if( obj instanceof Ref ) {
			Blob blob = blobGetter.get( ((Ref)obj).targetUri );
			if( blob == null ) throw new RuntimeException("Could not load " + ((Ref)obj).targetUri );
			return RDF.parseRdf(BlobUtil.getString(blob));
		} else if( obj instanceof RdfNode || obj instanceof String ) {
			return obj;
		} else if( obj == null ) {
			return null;
		} else {
			throw new RuntimeException("Could not get RdfNode from " + obj.getClass().getName());
		}
	}
	
	public void exportDirectoryEntry( String entryUri, RdfNode entry, File destDir ) {
		String fileType = (String)entry.getSingle(RDF.CCOUCH_OBJECTTYPE);
		String name = (String)entry.getSingle(RDF.CCOUCH_NAME);
		if( name.contains("/") || name.contains("\\") ) throw new RuntimeException("Invalid characters in directory entry name: " + name);
		File destination = new File(destDir + "/" + name);
		if( RDF.OBJECT_TYPE_FILE.equals(fileType) ) {
			Object content = entry.getSingle(RDF.CCOUCH_CONTENT);
			String lastModifiedStr = (String)entry.getSingle(RDF.DC_MODIFIED);
			if( content instanceof Ref ) {
				exportFile( ((Ref)content).targetUri, destination );
				if( lastModifiedStr != null ) {
					try {
						destination.setLastModified( RDF.CCOUCH_DATEFORMAT.parse(lastModifiedStr).getTime() );
					} catch (ParseException e) {
						throw new RuntimeException("Couldn't parse date in " + entryUri + ": " + lastModifiedStr );
					}
				}
			} else {
				throw new RuntimeException("File#content must be a ref in " + entryUri + ": " + content);
			}
		} else if( RDF.OBJECT_TYPE_DIRECTORY.equals(fileType) ) {
			Object listing =  entry.getSingle(RDF.CCOUCH_LISTING);
			if( listing instanceof Ref ) {
				exportDirectory(((Ref)listing).targetUri, destination);
			} else if( listing instanceof RdfNode ) {
				exportObjectFromRdf( entryUri, (RdfNode)listing, destination );
			} else {
				throw new RuntimeException("DirectoryEntry>listing must be a Ref or an RdfNode, but was " + listing);
			}
		} else {
			throw new RuntimeException("Invalid directory entry type in " + entryUri + ": " + fileType);
		}
	}
	
	public void exportObjectFromRdf( String listingUri, RdfNode listing, File destination ) {
		if( RDF.CCOUCH_COMMIT.equals(listing.typeName) || RDF.CCOUCH_REDIRECT.equals(listing.typeName) ) {
			String targetType = (String)listing.getSingle(RDF.CCOUCH_OBJECTTYPE);
			if( RDF.OBJECT_TYPE_FILE.equals(targetType) ) {
				Object targetRef = listing.getSingle(RDF.CCOUCH_CONTENT);
				if( targetRef instanceof Ref ) {
					String targetUri = ((Ref)targetRef).targetUri;
					exportFile(targetUri, destination);
				} else {
					throw new RuntimeException("Need a ref to export files, but found a " + targetRef.getClass().getName() + " in " + listingUri);
				}
			} else if( RDF.OBJECT_TYPE_DIRECTORY.equals(targetType) || RDF.OBJECT_TYPE_RDF.equals(targetType) ) {
				Object targetRef = listing.getSingle(RDF.CCOUCH_LISTING);
				Object target = getRdf(targetRef);
				String targetUri;
				if( targetRef instanceof Ref ) {
					targetUri = ((Ref)targetRef).targetUri;
				} else {
					targetUri = listingUri;
				}
				if( !(target instanceof RdfNode) ) {
					throw new RuntimeException( listingUri + " pointed to a " + target.getClass().getName() + " - RdfNode is required");
				}
				exportObjectFromRdf( targetUri, (RdfNode)target, destination );
			} else {
				throw new RuntimeException( "Don't know how to export object type " + targetType + " found in " + listingUri);
			}
		} else if( RDF.CCOUCH_DIRECTORYLISTING.equals(listing.typeName) ) {
			Collection c = (Collection)listing.getSingle(RDF.CCOUCH_ENTRIES);
			for( Iterator i=c.iterator(); i.hasNext(); ) {
				exportDirectoryEntry( listingUri, (RdfNode)i.next(), destination );
			}
		} else {
			throw new RuntimeException("The RDF document at " + listingUri + " does not contain a " +
					RDF.CCOUCH_DIRECTORYLISTING + " or a " +
					RDF.CCOUCH_COMMIT + ", but rather a " +
					listing.typeName + ", which I don't know how to export" );
		}
	}
	
	public void exportDirectory( String listingUri, File destination ) {
		Blob blob = blobGetter.get(listingUri);
		if( blob == null ) {
			throw new RuntimeException("Could not find directory listing: " + listingUri);
		}
		Object value = RDF.parseRdf(BlobUtil.getString(blob));
		if( !(value instanceof RdfNode) ) throw new RuntimeException("The RDF document at " + listingUri + " did not parse as an RDF object");
		exportObjectFromRdf( listingUri, (RdfNode)value, destination );
	}
	
	public static void main(String[] args) {
		String filename = "junk/export";
		
		File file = new File(filename);
		Exporter exporter = new Exporter(new Sha1BlobStore("junk-datastore"));
		
		exporter.exportDirectory("urn:sha1:FJOL7CP2VN72G6HOO3MBG4KUEYOCONID", file);
	}
}
