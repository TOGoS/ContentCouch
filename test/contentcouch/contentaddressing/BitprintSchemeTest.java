package contentcouch.contentaddressing;

import contentcouch.blob.BlobUtil;
import contentcouch.value.Blob;
import junit.framework.TestCase;

public class BitprintSchemeTest extends TestCase {
	public void testBitprintScheme() {
		Blob content = BlobUtil.getBlob("Hello, world!");
		
		byte[] bitprintHash = BitprintScheme.getInstance().getHash(content);
		String bitprintUrn = BitprintScheme.getInstance().hashToUrn(bitprintHash);
		assertTrue( bitprintUrn.startsWith("urn:bitprint:"));
		
		byte[] sha1Hash = Sha1Scheme.getInstance().getHash(content);
		String sha1Base32 = Sha1Scheme.getInstance().hashToRdfValue(sha1Hash);
		
		byte[] tigerTreeHash = TigerTreeScheme.getInstance().getHash(content);
		String tigerTreeBase32 = TigerTreeScheme.getInstance().hashToRdfValue(tigerTreeHash);

		System.err.println("Bitprint URN = " + bitprintUrn);
		System.err.println("  SHA-1 base32 = " + sha1Base32 );
		System.err.println("  TigerTree base32 = " + tigerTreeBase32 );
		
		assertEquals( sha1Base32, bitprintUrn.substring(13,13+32) );
		assertEquals( tigerTreeBase32, bitprintUrn.substring(13+32+1,13+32+1+39) );
		
		assertEquals( 13+32+1+39, bitprintUrn.length() );
	}
}
