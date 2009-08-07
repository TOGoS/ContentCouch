package contentcouch.blob;

import java.io.IOException;
import java.io.InputStream;

import togos.mf.value.Blob;


public class BlobInputStream extends InputStream {
	protected long coffset = 0;
	protected Blob blob;
	
	public BlobInputStream( Blob blob ) {
		this.blob = blob;
	}

	public int read() throws IOException {
		if( coffset >= blob.getLength() ) {
			return -1;
		}
		byte[] dat = blob.getData(coffset++, 1);
		return dat[0];
	}
	
	public int read(byte b[], int off, int len) throws IOException {
		if( len + coffset > blob.getLength() ) {
			len = (int)(blob.getLength() - coffset);
		}
		if( len <= 0 ) return -1;
		byte[] dat = blob.getData(coffset, len);
		for( int i=0; i<len; ++i ) {
			b[i+off] = dat[i];
		}
		coffset += len;
		return len;
	}
}