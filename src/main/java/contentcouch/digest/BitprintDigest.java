package contentcouch.digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bitpedia.util.Base32;
import org.bitpedia.util.TigerTree;

public class BitprintDigest extends MessageDigest
{
	public static String format( byte[] hash ) {
		byte[] sha1Hash = new byte[20];
		System.arraycopy( hash, 0, sha1Hash, 0, 20);
		byte[] tigerTreeHash = new byte[24];
		System.arraycopy( hash, 20, tigerTreeHash, 0, 24);
		return Base32.encode(sha1Hash) + "." + Base32.encode(tigerTreeHash);
	}
	
	MessageDigest sha1;
	TigerTree tt;
	
	public BitprintDigest() {
		super("Bitprint");
		try {
			sha1 = MessageDigest.getInstance("SHA-1");
		} catch( NoSuchAlgorithmException e ) {
			throw new RuntimeException("Apparently SHA-1 isn't available", e);
		}
		tt   = new TigerTree();
	}
	
	protected byte[] joinHashes( byte[] sha1Hash, byte[] tigerTreeHash ) {
		byte[] hash = new byte[44];
		System.arraycopy( sha1Hash, 0, hash, 0, 20);
		System.arraycopy( tigerTreeHash, 0, hash, 20, 24);
		return hash;
	}
	
	protected byte[] engineDigest() {
		return joinHashes( sha1.digest(), tt.digest() ); 
	}
	
	protected void engineReset() {
		sha1.reset();
		tt.reset();
	}
	
	protected void engineUpdate( byte i ) {
		sha1.update(i);
		tt.update(i);
	}
	
	protected void engineUpdate( byte[] input, int offset, int len ) {
		sha1.update(input, offset, len);
		tt.update(input, offset, len);
	}
}
