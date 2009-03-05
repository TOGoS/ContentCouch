package contentcouch.blob;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import contentcouch.value.Blob;
import contentcouch.value.MetadataHaver;

public class FileCacheBlob implements Blob, MetadataHaver {
	File file;
	RandomAccessFile raf;
	Blob sourceBlob;
	long bytesCached = 0;
	Map metadata;
	long length = -2;
	
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
	
	public Map getMetadata() { return metadata; }
	
	public Object getMetadata(String key) {
		if( metadata == null ) return null;
		return metadata.get(key);
	}
	
	public void putMetadata(String key, Object value) {
		if( metadata == null ) metadata = new HashMap();
		metadata.put(key,value);
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

	protected long cacheAll() {
		try {
			raf.seek(bytesCached);
			if( !(sourceBlob instanceof InputStreamBlob) ) {
				throw new RuntimeException("cacheAll only supported when source is InputStreamBlob");
			}
			InputStreamBlob isb = (InputStreamBlob)sourceBlob;
			if( isb.getPosition() != bytesCached ) {
				throw new RuntimeException("bytesCached != sourceBlob.position");
			}
			raf.seek(bytesCached);
			int nread;
			byte[] b = new byte[1024];
			while( (nread = isb.read(b, 0, 1024)) > 0 ) {
				bytesCached += nread;
				raf.write(b, 0, nread);
			}
			return bytesCached;
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public long getLength() {
		if( length < 0 ) {
			length = sourceBlob.getLength();
			if( length == -1 && sourceBlob instanceof InputStreamBlob ) {
				cacheAll();
				length = bytesCached;
			}
		}
		return length;
	}
	
	public void finalize() throws IOException {
		if( raf != null ) raf.close(); 
		if( file != null ) file.delete();
	}
}
