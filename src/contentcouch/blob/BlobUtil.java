package contentcouch.blob;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import contentcouch.file.FileBlob;
import contentcouch.file.FileUtil;
import contentcouch.rdf.RdfNamespace;
import contentcouch.value.Blob;

public class BlobUtil {
	//// Conversions to Bytes ////
	
	public static byte[] getBytes(Blob blob) {
		long len = blob.getLength();
		if( len > Integer.MAX_VALUE ) {
			throw new RuntimeException("Blob is too big to turn into bytes string: " + len + " bytes");
		}
		return blob.getData(0, (int)blob.getLength());
	}
	
	//// Conversion to String ////
	
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
	
	public static String getString(Object obj) {
		if( obj instanceof String ) return (String)obj;
		if( obj instanceof byte[] ) return getString((byte[])obj);
		if( obj instanceof Blob ) return getString((Blob)obj);
		return obj.toString();
	}
	
	//// Conversions to Blob ////
	
	public static ByteArrayBlob getBlob(String s) {
		try {
			ByteArrayBlob bab = new ByteArrayBlob(s.getBytes("UTF-8"));
			bab.putMetadata(RdfNamespace.DC_FORMAT, "text/plain");
			return bab;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Blob getBlob(Object obj) {
		if( obj == null ) {
			return null;
		} else if( obj instanceof Blob ) {
			return (Blob)obj;
		} else if( obj instanceof byte[] ) {
			return new ByteArrayBlob((byte[])obj);
		} else if( obj instanceof String ) {
			return getBlob((String)obj);
		} else if( obj instanceof File ) {
			return new FileBlob((File)obj);
		} else {
			throw new RuntimeException("Don't know how to turn " + obj.getClass().getName() + " into a Blob");
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
			if( blob instanceof File ) {
				FileInputStream is = new FileInputStream((File)blob);
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
		FileUtil.mkParentDirs(f);
		try {
			FileOutputStream fos = new FileOutputStream(f);
			try {
				writeBlobToOutputStream(blob, fos);
			} finally {
				fos.close();
			}
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
}
