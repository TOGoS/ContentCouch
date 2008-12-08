package contentcouch.data;

import java.util.Arrays;

public class ByteArrayBlob implements Blob {
	byte[] bytes;
	int offset;
	int length;
	
	public ByteArrayBlob( byte[] bytes, int offset, int length ) {
		this.bytes = bytes;
		this.offset = offset;
		this.length = length;
	}

	public ByteArrayBlob( byte[] bytes ) {
		this( bytes, 0, bytes.length ); 
	}
	
	public long getLength() {
		return length;
	}
	
	public byte[] getData(long offset, int length) {
		if( this.offset == 0 && this.length == bytes.length && offset == 0 & length == this.length ) return bytes;
		return Arrays.copyOfRange(bytes, (int)offset+this.offset, length);
	}
	
	public byte[] getBackingData() { return bytes; }
	public int getBackingOffset() { return offset; }
}
