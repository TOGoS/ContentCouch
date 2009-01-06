package contentcouch.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import contentcouch.data.Blob;
import contentcouch.data.BlobUtil;
import contentcouch.data.FileBlob;
import contentcouch.store.BlobSink;
import contentcouch.store.FileGetter;
import contentcouch.store.Sha1BlobStore;
import contentcouch.xml.RDF;
import contentcouch.xml.RDF.RdfNode;

public class Importer {
	BlobSink blobSink;
	
	public Importer( BlobSink blobSink ) {
		this.blobSink = blobSink;
	}
	
	protected String uriEscapePath( String path ) {
		// TODO
		return path;
	}
	
	protected String getFileUri(File file ) {
		try {
			String path = file.getCanonicalPath();
			path = path.replace('\\', '/');
			if( path.charAt(1) == ':' ) {
				// Windows path!
				return "file:///" + uriEscapePath(path);
			} else if( path.charAt(0) == '/' ) { 
				// Unix path, leading slash already included!
				return "file://" + uriEscapePath(path);
			} else {
				return "file:" + uriEscapePath(path);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected Blob createMetadataBlob( RDF.RdfNode desc, String defaultNamespace ) {
		return BlobUtil.getBlob(RDF.xmlEncodeRdf(desc, defaultNamespace));
	}
	
	protected Blob createMetadataBlob( String aboutUri, Map properties ) {
		RDF.Description desc = new RDF.Description();
		desc.importValues(properties);
		desc.about = new RDF.Ref(aboutUri);
		return createMetadataBlob(desc, null);
	}
	
	public String importContent( Blob b ) {
		return blobSink.push( b );
	}
	
	public String importFileContent( File file, FileImportListener fileImportListener ) {
		String contentUri = importContent( new FileBlob(file) );
		if( fileImportListener != null ) {
			fileImportListener.fileImported( file, contentUri );
		}
		return contentUri;
	}
	
	public File getFile( String uri ) {
		if( blobSink instanceof FileGetter ) {
			return ((FileGetter)blobSink).getFile(uri);
		} else {
			return null;
		}
	}
	
	public RdfNode getDirectoryEntryRdfNode( File file, FileImportListener fileImportListener ) {
		RdfNode n = new RdfNode();
		n.typeName = RDF.CCOUCH_DIRECTORYENTRY;
		if( file.isDirectory() ) {
			n.add(RDF.CCOUCH_FILETYPE, "Directory");
			n.add(RDF.CCOUCH_NAME, file.getName());
			n.add(RDF.CCOUCH_LISTING, new RDF.Ref(importDirectory(file, fileImportListener)));
		} else {
			n.add(RDF.CCOUCH_FILETYPE, "File");
			n.add(RDF.CCOUCH_NAME, file.getName());
			n.add(RDF.DC_MODIFIED, RDF.CCOUCH_DATEFORMAT.format(new Date(file.lastModified())));
			n.add(RDF.CCOUCH_CONTENT, new RDF.Ref(importFileContent(file, fileImportListener)));
		}
		return n;
	}

	public String importDirectory( File dir, FileImportListener fileImportListener ) {
		if( !dir.isDirectory() ) {
			throw new RuntimeException("Cannot import a plain file with importDir!");
		}
		RdfNode n = new RdfNode();
		n.typeName = RDF.CCOUCH_DIRECTORYLISTING;
		List entries = new ArrayList();
		File[] subFiles = dir.listFiles();
		for( int i=0; i<subFiles.length; ++i ) {
			File subFile = subFiles[i];
			if( subFile.getName().startsWith(".") ) continue;
			entries.add( getDirectoryEntryRdfNode(subFile, fileImportListener) );
		}
		n.add(RDF.CCOUCH_ENTRIES, entries);
		String listingUri = importContent(createMetadataBlob(n, RDF.CCOUCH_NS));
		if( fileImportListener != null ) {
			fileImportListener.fileImported( dir, "@" + listingUri );
		}
		return listingUri;
	}
	
	public String importFileOrDirectory( File file, FileImportListener fileImportListener ) {
		if( file.isDirectory() ) {
			return "@" + importDirectory( file, fileImportListener );
		} else {
			return importFileContent( file, fileImportListener );
		}
	}
	
	public void recursivelyImportFiles(File dir, FileImportListener fileImportListener) {
		if( dir.isFile() ) {
			importFileContent(dir, fileImportListener);
		} else {
			String[] subNames = dir.list();
			for( int i=0; i<subNames.length; ++i ) {
				String subName = subNames[i];
				if( subName.startsWith(".") ) continue;
				recursivelyImportFiles(new File(dir + "/" + subName), fileImportListener);
			}
		}
	}
	
	public static void main(String[] argc) {
		String filename = "junk/import";
		
		File file = new File(filename);
		Importer importer = new Importer(new Sha1BlobStore("junk-datastore"));
		//importer.recursivelyImportFiles(file);
		
		String ref = importer.importDirectory(file, null);
		System.out.println(ref);
	}
}
