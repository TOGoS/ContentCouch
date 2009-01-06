package contentcouch.digest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.bitpedia.util.Base32;

import contentcouch.data.Blob;
import contentcouch.data.ByteArrayBlob;
import contentcouch.data.FileBlob;

public class DigestUtil {
	final static int maxChunkLength = 1024*1024; // 1 megabyte
	
	protected static byte[] digestFile( File file, MessageDigest md ) {
		try {
			FileInputStream fis = new FileInputStream(file);
			try {
				byte[] buffer = new byte[maxChunkLength];
				int r;
				while( (r = fis.read(buffer)) > 0 ) {
					if( r == buffer.length ) {
						md.update(buffer);
					} else {
						md.update(Arrays.copyOf(buffer, r));
					}
				}
			} finally {
				fis.close();
			}
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
		return md.digest();
	}
	
	public static byte[] digestBlob( Blob blob, String digestAlgorithmName )
		throws NoSuchAlgorithmException
	{
		MessageDigest md = MessageDigest.getInstance(digestAlgorithmName);
		
		if( blob instanceof ByteArrayBlob ) {
			// Do all at once no matter the length
			return md.digest( blob.getData(0, (int)blob.getLength()) );
		} else if( blob instanceof FileBlob ) {
			// Use special file reading function so not to do many opens and closes 
			return digestFile( ((FileBlob)blob).getFile(), md );
		} else {
			// Do it one chunk at a time ._.
			long len = blob.getLength();
			long done;
			int chunkLength;
			for( done = 0; done < len; done += chunkLength ) {
				chunkLength = (int)((done + maxChunkLength < len) ? maxChunkLength : len - done);
				md.update(blob.getData(done, chunkLength));
			}
			return md.digest();
		}
	}
	
	public static byte[] sha1DigestBlob( Blob blob ) {
		try {
			return digestBlob( blob, "SHA-1" );
		} catch( NoSuchAlgorithmException e ) {
			throw new RuntimeException( e );
		}
	}
	
	public static final char numToLowerHexDigit( int d ) {
		if( d >= 10 ) return (char)('a' - 10 + d);
		return (char)('0' + d);
	}
	
	public static final int hexDigitToNum( char d ) {
		if( d >= 'a' ) return 10 + d - 'a';
		if( d >= 'A' ) return 10 + d - 'A';
		return d - '0';
	}
	
	public static char[] bytesToLowerHex( byte[] bytes ) {
		char[] chars = new char[bytes.length<<1]; 
		for( int i=0; i<bytes.length; ++i ) {
			chars[(i<<1)+0] = numToLowerHexDigit((bytes[i] >> 4) & 0xF);
			chars[(i<<1)+1] = numToLowerHexDigit((bytes[i] >> 0) & 0xF);
		}
		return chars;
	}
	
	public static byte[] hexToBytes( char[] digits ) {
		if( digits.length %2 != 0 ) {
			throw new RuntimeException( "Odd number of hex digits - cannot convert to byte array" );
		}
		byte[] bytes = new byte[digits.length>>1];
		for( int i=0; i<bytes.length; ++i ) {
			bytes[i] = (byte)((hexDigitToNum(digits[i<<1]) << 4) | (hexDigitToNum(digits[(i<<1)+1])));
		}
		return bytes;
	}
	
	public static String getSha1Urn( Blob b ) {
		return "urn:sha1:" + Base32.encode(sha1DigestBlob(b));
	}
	
	public static void main(String[] args) {
		System.out.println( bytesToLowerHex(sha1DigestBlob(new FileBlob(new File("C:/junk/xvi32.zip")))) );
	}
}