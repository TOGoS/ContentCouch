package contentcouch.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileBlob implements Blob {
	File file;
	
	public FileBlob( File file ) {
		this.file = file;
	}
	
	public File getFile() {
		return file;
	}
	
	public byte[] getData(long offset, int length) {
		try {
			FileInputStream s = new FileInputStream(file);
			s.skip(offset);
			byte[] data = new byte[length];
			int read = 0;
			int nuread = 1;
			while( nuread > 0 ) {
				nuread = s.read(data, read, (int)(length-read));
				if( nuread > 0 ) read += nuread;
			}
			return data;
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public long getLength() {
		return file.length();
	}
}
