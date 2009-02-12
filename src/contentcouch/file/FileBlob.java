package contentcouch.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import contentcouch.value.Blob;

public class FileBlob extends File implements Blob {
	public FileBlob( File file ) {
		super(file.getPath());
	}
	
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
