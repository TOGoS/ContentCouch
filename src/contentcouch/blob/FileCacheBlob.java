package contentcouch.blob;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import contentcouch.value.Blob;

public class FileCacheBlob implements Blob {
	File file;
	RandomAccessFile raf;
	Blob sourceBlob;
	long bytesCached = 0;
	
	public FileCacheBlob( File file, Blob sourceBlob ) {
		this.file = file;
		this.file.deleteOnExit();
		this.sourceBlob = sourceBlob;
		try {
			this.raf = new RandomAccessFile(file, "rw");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	public byte[] getData(long offset, int length) {
		if( offset < 0 || offset+length > getLength() ) {
			throw new ArrayIndexOutOfBoundsException("Bad offset/length given for blob of length " + getLength() + ": " + offset + "+" + length );
		}

		try {
			int chunkSize = 1024*8;
			while( bytesCached < offset+length ) {
				raf.seek(bytesCached);
				byte[] data = sourceBlob.getData(bytesCached, (int)(bytesCached + chunkSize > offset + length ? offset + length - bytesCached : chunkSize) );
				raf.write(data);
				bytesCached += data.length;
			}
		
			raf.seek(offset);
			byte[] data = new byte[length];
			raf.readFully(data, 0, length);
			return data;
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	public long getLength() {
		return sourceBlob.getLength();
	}
	
	public void finalize() throws IOException {
		if( raf != null ) raf.close(); 
		if( file != null ) file.delete();
	}
}
