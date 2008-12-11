package contentcouch.app;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import contentcouch.data.Blob;
import contentcouch.data.ByteArrayBlob;
import contentcouch.data.FileBlob;
import contentcouch.metadata.RDF;

public class Importer {
	Datastore datastore;
	
	public String collector;
	public String[] tags;
	public Map mimeTypesByExt;
	public String defaultMimeType;
	
	public DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public Importer( Datastore datastore ) {
		this.datastore = datastore;
		this.mimeTypesByExt = new HashMap();
		mimeTypesByExt.put("txt", "text/plain");
		mimeTypesByExt.put("html", "text/html");
		mimeTypesByExt.put("jpg", "image/jpeg");
		mimeTypesByExt.put("png", "image/png");
		mimeTypesByExt.put("gif", "image/gif");
		mimeTypesByExt.put("mp3", "audio/mpeg");
		mimeTypesByExt.put("rdf", "application/rdf+xml");
	}
	
	protected String getFileUri(File file ) {
		try {
			String path = file.getCanonicalPath();
			path = path.replace('\\', '/');
			if( path.charAt(1) == ':' ) {
				// Windows path!
				return "file:///" + path;
			} else {
				// Unix path, leading slash already included!
				return "file://" + path;				
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected String getMimeType( File file ) {
		String nam = file.getName();
		int lastDot = nam.lastIndexOf('.');
		if( lastDot == -1 || lastDot == nam.length()-1 ) return null;
		String ext = nam.substring(lastDot+1).toLowerCase();
		String mimeType = (String)mimeTypesByExt.get(ext);
		if( mimeType != null ) return mimeType;
		return defaultMimeType;
	}
	
	public Map getFileMetadata( File file ) {
		Map metadata = new HashMap();
		metadata.put(RDF.CCOUCH_COLLECTOR, collector);
		metadata.put(RDF.CCOUCH_NAME, file.getName());
		metadata.put(RDF.DC_MODIFIED, dateFormat.format(new Date(file.lastModified())));
		
		String mimeType = getMimeType( file );
		if( mimeType != null ) metadata.put(RDF.DC_FORMAT, mimeType);
		
		for( int i=0; tags != null && i<tags.length; ++i ) {
			metadata.put(RDF.CCOUCH_TAG, tags[i]);
		}
		return metadata;
	}
	
	public Map getMetametadata( File file ) {
		Map metametadata = new HashMap();
		metametadata.put(RDF.CCOUCH_IMPORTEDDATE, dateFormat.format(new Date()));
		metametadata.put(RDF.CCOUCH_IMPORTEDFROM, getFileUri(file));
		metametadata.put(RDF.DC_FORMAT, "application/rdf+xml");
		return metametadata;
	}

	protected Blob createMetadataBlob( RDF.Description desc ) {
		return new ByteArrayBlob(RDF.xmlEncodeRdf(desc).getBytes());
	}
	
	protected Blob createMetadataBlob( String aboutUri, Map properties ) {
		return createMetadataBlob(new RDF.Description(new RDF.Ref(aboutUri), properties));
	}
	
	public void importFile(File file) {
		String contentUri = datastore.push( "data", new FileBlob(file) );
		if( contentUri == null ) return;
		Map metadata = getFileMetadata( file );
		if( metadata == null || metadata.size() == 0 ) return;
		String metadataUri = datastore.push( "metadata", createMetadataBlob(contentUri, metadata) );
		if( metadataUri == null ) return;
		Map metametadata = getMetametadata( file );
		if( metametadata == null || metametadata.size() == 0 ) return;
		datastore.push("metametadata", createMetadataBlob(metadataUri, metametadata));
	}
	
	public void recursivelyImport(File dir) {
		if( dir.isFile() ) {
			importFile(dir);
		} else {
			String[] subNames = dir.list();
			for( int i=0; i<subNames.length; ++i ) {
				String subName = subNames[i];
				if( subName.startsWith(".") ) continue;
				recursivelyImport(new File(dir + "/" + subName));
			}
		}
	}

	public static void main(String[] argc) {
		String dirname = "junk";
		
		File f = new File(dirname);
		Importer importer = new Importer(new Datastore("junk-datastore"));
		importer.recursivelyImport(f);
	}
}
