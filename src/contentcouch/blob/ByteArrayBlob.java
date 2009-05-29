package contentcouch.blob;

import contentcouch.value.Blob;

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
	
	//// Blob implementation ////
	
	public long getLength() {
		return length;
	}
	
	public byte[] getData(long offset, int length) {
		if( this.offset == 0 && this.length == bytes.length && offset == 0 & length == this.length ) return bytes;
		byte[] result = new byte[length];
		for( int i=0, j=(int)offset; i<length; ++i, ++j ) {
			result[i] = bytes[j];
		}
		return result;
	}
	
	public byte[] getBackingData() { return bytes; }
	public int getBackingOffset() { return offset; }
}
