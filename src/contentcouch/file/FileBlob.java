package contentcouch.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import contentcouch.rdf.RdfNamespace;
import contentcouch.value.Blob;
import contentcouch.value.MetadataHaver;

public class FileBlob extends File implements Blob, MetadataHaver {
	Map metadata;
	
	public FileBlob( File file ) {
		super(file.getPath());
		putMetadata(RdfNamespace.DC_MODIFIED, new Date(lastModified()));
	}
	
	//// MetadataHaver implementation ////
	
	public Map getMetadata() { return metadata; }
	
	public void putMetadata(String key, Object value) {
		if( metadata == null ) metadata = new HashMap();
		metadata.put(key,value);
	}
	
	//// Blob implementation ////
	
	public byte[] getData(long offset, int length) {
		try {
			FileInputStream s = new FileInputStream(this);
			try {
				s.skip(offset);
				byte[] data = new byte[length];
				int read = 0;
				int nuread = 1;
				while( nuread > 0 ) {
					nuread = s.read(data, read, (int)(length-read));
					if( nuread > 0 ) read += nuread;
				}
				return data;
			} finally {
				s.close();
			}
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public long getLength() {
		return length();
	}
}
