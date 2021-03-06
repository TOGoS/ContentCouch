package contentcouch.blob;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import contentcouch.stream.StreamUtil;



public class InputStreamBlob implements Blob {
	InputStream inputStream;
	long length;
	long position = 0;
	byte[] previousRead = null;
	
	public InputStreamBlob(InputStream is, long length) {
		this.inputStream = is;
		this.length = length;
	}
	
	public byte[] getData(long offset, int length) {
		try {
			if( offset < 0 || offset+length > this.length ) throw new RuntimeException("Requested data out of bounds!");
			if( previousRead == null && offset < position ) throw new RuntimeException("Can't rewind to get data at " + offset);
			if( previousRead != null && offset < position-previousRead.length ) throw new RuntimeException("Can't rewind to get data at " + offset + " (previous read at " + (position-previousRead.length) + "+" + previousRead.length + ")");
			if( offset > position ) inputStream.skip(offset-position);
			byte[] data = new byte[length];
			int read;
			if( offset < position ) {
				read = (int)(position-offset);
				for( int i=0; i<read; ++i ) {
					data[i] = previousRead[previousRead.length-read+i];
				}
			} else {
				read = 0;
			}
			while( read < length ) {
				int nread = inputStream.read(data,read,length-read);
				if( nread == -1 ) {
					throw new RuntimeException(new IOException("Reached end of input stream before expected length: tried to read " + read + "+" + (length-read) + " of total " + this.length));
				}
				read += nread;
			}
			previousRead = data;
			position += read;
			return data;
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	public long getLength() {
		return length;
	}
	
	// For getting at the underlying input stream

	public long getPosition() {
		return position;
	}
	
	public int read(byte[] bytes, int off, int length) throws IOException{
		int r = inputStream.read(bytes, off, length);
		position += r;
		return r;
	}
	
	public void writeTo( OutputStream os, long offset ) throws IOException {
		if( previousRead == null && offset < position ) throw new RuntimeException("Can't rewind to get data at " + offset);
		if( previousRead != null && offset < position-previousRead.length ) throw new RuntimeException("Can't rewind to get data at " + offset + " (previous read at " + (position-previousRead.length) + "+" + previousRead.length + ")");
		if( offset < position ) {
			//   |             |XXX|YYYYYYYYYYY|            |
			//   0                 offset      position     length
			//                 position-previousRead.length
			//   XXX = offset - (position-previousRead.length)
			//   YYY = previousRead.length - (offset - (position-previousRead.length))
			//       = previousRead.length - offset + position -previousRead.length
			//       = position - offset
			os.write(previousRead, (int)(offset-(position-previousRead.length)), (int)(position-offset));
		}
		StreamUtil.copyInputToOutput(inputStream, os);
		/*
		int br;
		byte[] readed = new byte[BlobUtil.READ_CHUNK_SIZE];
		while( (br = inputStream.read(readed)) > 0 ) {
			position += br;
			os.write(readed, 0, br);
		}
		*/
	}
}
