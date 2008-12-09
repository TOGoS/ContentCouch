package contentcouch.app;

import java.io.File;
import java.util.Map;

import contentcouch.data.Blob;

public class Importer {
	public Importer( Datastore datastore ) {
		
	}
	
	public Blob metadataToRdfBlob( String about, Map metadata ) {
		return null;
		//return new ByteArrayBlob()
	}
	
	public void push( Blob content, Map metadata, Map metametadata ) {
		
	}
	
	public void pushMetadata( Blob metadata, Map metametadata ) {
		
	}
	
	public void recursivelyImport(File dir) {
		
	}
	
	public static void main(String[] argc) {
		String dirname = ".";
		
		File f = new File(dirname);
		//f.listFiles()
	}
}
