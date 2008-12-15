package contentcouch.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class BlobUtil {
	public static byte[] getBytes(Blob blob) {
		long len = blob.getLength();
		if( len > Integer.MAX_VALUE ) {
			throw new RuntimeException("Blob is too big to turn into bytes string: " + len + " bytes");
		}
		return blob.getData(0, (int)blob.getLength());
	}
	
	public static String getString(byte[] bytes) {
		try {
			return new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String getString(Blob blob) {
		return getString(getBytes(blob));
	}
	
	public static Blob getBlob(String s) {
		try {
			return new ByteArrayBlob(s.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	static final int maxChunkLength = 1024*1024;

	public static void copyInputToOutput( InputStream is, OutputStream os )
		throws IOException
	{
		byte[] bytes = new byte[maxChunkLength];
		int read;
		while( (read = is.read(bytes)) > 0 ) {
			os.write(bytes, 0, read);
		}
	}
	
	public static void writeBlobToOutputStream( Blob blob, OutputStream os ) {
		try {
			if( blob instanceof FileBlob ) {
				FileInputStream is = new FileInputStream(((FileBlob)blob).getFile());
				try {
					copyInputToOutput(is, os);
				} finally {
					is.close();
				}
			} else if( blob instanceof ByteArrayBlob ) {
				os.write(((ByteArrayBlob)blob).bytes);
			} else {
				long len = blob.getLength();
				for( long i=0; i<len; i+=maxChunkLength ) {
					os.write(blob.getData(i, (int)(len > i+maxChunkLength ? maxChunkLength : len-i) ));
				}
			}
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public static void writeBlobToFile( Blob blob, File f ) {
		File d = f.getParentFile();
		if( d != null && !d.exists() ) {
			if( !d.mkdirs() ) {
				throw new RuntimeException("Couldn't create parent dirs for " + f);
			}
		}
		try {
			FileOutputStream fos = new FileOutputStream(f);
			writeBlobToOutputStream(blob, fos);
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
}
