package contentcouch.app;

import java.io.File;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;

import contentcouch.data.Blob;
import contentcouch.data.BlobUtil;
import contentcouch.store.BlobSource;
import contentcouch.xml.RDF;
import contentcouch.xml.RDF.RdfNode;
import contentcouch.xml.RDF.Ref;

public class Exporter {
	BlobSource blobSource;
	
	public Exporter( BlobSource blobSource ) {
		this.blobSource = blobSource;
	}

	public void exportFile( String fileUri, File destination ) {
		Blob blob = blobSource.get(fileUri);
		if( blob == null ) {
			throw new RuntimeException("Couldn't find blob: " + fileUri);
		}
		BlobUtil.writeBlobToFile(blob, destination);
	}
	
	public void exportDirectoryEntry( String entryUri, RdfNode entry, File destDir ) {
		String fileType = (String)entry.getSingle(RDF.CCOUCH_FILETYPE);
		String name = (String)entry.getSingle(RDF.CCOUCH_NAME);
		if( name.contains("/") || name.contains("\\") ) throw new RuntimeException("Invalid characters in directory entry name: " + name);
		File destination = new File(destDir + "/" + name);
		if( "File".equals(fileType) ) {
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
		} else if( "Directory".equals(fileType) ) {
			Object listing =  entry.getSingle(RDF.CCOUCH_LISTING);
			if( listing instanceof Ref ) {
				exportDirectory(((Ref)listing).targetUri, destination);
			} else if( listing instanceof RdfNode ) {
				exportDirectory( entryUri, (RdfNode)listing, destination );
			} else {
				throw new RuntimeException("DirectoryEntry>listing must be a Ref or an RdfNode, but was " + listing);
			}
		} else {
			throw new RuntimeException("Invalid directory entry type in " + entryUri + ": " + fileType);
		}
	}
	
	public void exportDirectory( String listingUri, RdfNode listing, File destination ) {
		if( !RDF.CCOUCH_DIRECTORYLISTING.equals(listing.typeName) ) {
			throw new RuntimeException("The RDF document at " + listingUri + " does not contain a " +
					RDF.CCOUCH_DIRECTORYLISTING + ", but a " + listing.typeName );
		}
		Collection c = (Collection)listing.getSingle(RDF.CCOUCH_ENTRIES);
		for( Iterator i=c.iterator(); i.hasNext(); ) {
			exportDirectoryEntry( listingUri, (RdfNode)i.next(), destination );
		}
	}
	
	public void exportDirectory( String listingUri, File destination ) {
		Blob blob = blobSource.get(listingUri);
		if( blob == null ) {
			throw new RuntimeException("Could not find directory listing: " + listingUri);
		}
		Object value = RDF.parseRdf(BlobUtil.getString(blob));
		if( !(value instanceof RdfNode) ) throw new RuntimeException("The RDF document at " + listingUri + " did not parse as an RDF object");
		exportDirectory( listingUri, (RdfNode)value, destination );
	}
	
	public static void main(String[] args) {
		String filename = "junk/export";
		
		File file = new File(filename);
		Exporter exporter = new Exporter(new Datastore("junk-datastore"));
		
		exporter.exportDirectory("urn:sha1:FJOL7CP2VN72G6HOO3MBG4KUEYOCONID", file);
	}
}
