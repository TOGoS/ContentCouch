package contentcouch.blob;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import contentcouch.app.Linker;
import contentcouch.file.FileBlob;
import contentcouch.file.FileUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.RdfNode;
import contentcouch.stream.StreamUtil;

public class BlobUtil {
	//// Convert blobs to other things ////
	
	public static byte[] getBytes(Blob blob) {
		long len = blob.getLength();
		if( len > Integer.MAX_VALUE ) {
			throw new RuntimeException("Blob is too big to turn into bytes string: " + len + " bytes");
		}
		return blob.getData(0, (int)blob.getLength());
	}
	
	public static String getString(Blob blob) {
		return ValueUtil.getString(getBytes(blob));
	}
	
	public static InputStream getInputStream( Blob b ) {
		if( b instanceof FileBlob ) {
			try {
				return new FileInputStream((FileBlob)b);
			} catch( FileNotFoundException e ) {
				throw new RuntimeException(e);
			}
		} else if( b instanceof ByteArrayBlob ) {
			return new ByteArrayInputStream(((ByteArrayBlob)b).getBackingData());
		} else {
			return new BlobInputStream( b );
		}
	}
	
	//// Convert things to blobs ////
	
	public static ByteArrayBlob getBlob(String s) {
		return new ByteArrayBlob(ValueUtil.getBytes(s));
	}

	public static Blob getBlob(Object obj, boolean failOnFailure) {
		if( obj == null ) {
			return null;
		} else if( obj instanceof Blob ) {
			return (Blob)obj;
		} else if( obj instanceof byte[] ) {
			return new ByteArrayBlob((byte[])obj);
		} else if( obj instanceof String ) {
			return getBlob((String)obj);
		} else if( obj instanceof RdfNode ) {
			return getBlob( obj.toString() );
		} else if( obj instanceof File ) {
			File f = (File)obj;
			if( f.exists() && f.isFile() ) {
				return new FileBlob(f);
			}
			if( failOnFailure ) throw new RuntimeException("Don't know how to directory File into a Blob");
			return null;
		}
		
		if( failOnFailure ) throw new RuntimeException("Don't know how to turn " + obj.getClass().getName() + " into a Blob");
		return null;
	}
	
	public static Blob getBlob(Object obj) {
		return getBlob(obj, true);
	}
	
	public static final int READ_CHUNK_SIZE = 8*1024;

	public static Blob readInputStreamIntoBlob(InputStream inputStream, long maxLength)
		throws IOException 
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] data = new byte[READ_CHUNK_SIZE];
		int read;
		long totalRead = 0;
		do {
			long readThisTime = maxLength - totalRead;
			if( readThisTime > data.length ) readThisTime = data.length;
			if( readThisTime == 0 ) break;
			read = inputStream.read(data, 0, (int)readThisTime );
			if( read == -1 ) break;
			totalRead += read;
			baos.write(data, 0, read);
		} while( read > 0 );
		return new ByteArrayBlob( baos.toByteArray() );
	}
	
	public static void writeBlobToOutputStream( Blob blob, OutputStream os )
		throws IOException
	{
		if( blob instanceof InputStreamBlob ) {
			((InputStreamBlob)blob).writeTo(os, 0);
		} else if( blob instanceof File ) {
			FileInputStream is = new FileInputStream((File)blob);
			try {
				StreamUtil.copyInputToOutput(is, os);
			} finally {
				is.close();
			}
		} else if( blob instanceof ByteArrayBlob ) {
			os.write(((ByteArrayBlob)blob).bytes);
		} else {
			long len = blob.getLength();
			for( long i=0; i<len; i+=READ_CHUNK_SIZE ) {
				byte[] chunk = blob.getData(i, (int)(len > i+READ_CHUNK_SIZE ? READ_CHUNK_SIZE : len-i));
				os.write( chunk );
			}
		}
	}
	
	// Protected because you should be using the atomic version
	protected static void writeBlobToFile( Blob blob, File f ) {
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
	
	public static void writeBlobToFileAtomically( Blob blob, File destFile ) {
		File destDir = destFile.getParentFile();
		File tempFile = null;
		try {
			tempFile = File.createTempFile(".cc-"+destFile.getName()+"-", ".temp", destDir);
			writeBlobToFile(blob, tempFile);
			if( !tempFile.renameTo(destFile) ) {
				throw new RuntimeException("Failed to move '"+tempFile+"' to '"+destFile+"'");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if( tempFile != null && tempFile.exists() ) {
				if( !tempFile.delete() ) {
					System.err.println("Failed to delete temp file '"+tempFile+"'");
				}
			}
		}
	}

	public static void linkBlobToFile( Blob blob, File f ) {
		if( blob instanceof File ) {
			Linker.getInstance().link((File)blob, f);
		} else {
			writeBlobToFile(blob, f);
		}
	}
	
	public static int compareBlobs( Blob b1, Blob b2 ) {
		long l1 = b1.getLength();
		long l2 = b2.getLength();
		long minLength;
		minLength = ( l1 < l2 ) ? l1 : l2;
		long ran = 0;
		int chunkSize;
		if( b1 instanceof ByteArrayBlob && b2 instanceof ByteArrayBlob ) chunkSize = (int)(b1.getLength());
		else chunkSize = READ_CHUNK_SIZE;
		while( ran < minLength ) {
			int cl = (int)((minLength - ran) < chunkSize ? (minLength - ran) : chunkSize);
			byte[] chunk1 = b1.getData(ran, cl);
			byte[] chunk2 = b2.getData(ran, cl);
			for( int i=0; i<cl; ++i ) {
				if( chunk1[i] == chunk2[i] ) continue;
				if( chunk1[i] < chunk2[i] ) return -1;
				return 1;
			}
			ran += cl;
		}
		if( l1 == l2 ) return 0;
		if( l1 < l2 ) return -1;
		return 1;
	}
	
	public static boolean blobsEqual( Blob b1, Blob b2 ) {
		// TODO maybe: If both file blobs and a repo set up, use their cached hashes?
		return compareBlobs(b1, b2) == 0;
	}
}
