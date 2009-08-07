package contentcouch.contentaddressing;

import togos.mf.value.Blob;
import contentcouch.blob.BlobUtil;
import contentcouch.contentaddressing.BitprintScheme.Bitprint;
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
	
	public void testEquivalence() {
		String sha1a = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		String sha1b = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";
		String tta  = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";
		String ttb  = "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD";
		
		// Compare SHA-1 URNs (true/false)
		assertEquals( Boolean.TRUE, Bitprint.getUriEquivalence("urn:sha1:"+sha1a, "urn:sha1:"+sha1a) );
		assertEquals( Boolean.FALSE, Bitprint.getUriEquivalence("urn:sha1:"+sha1a, "urn:sha1:"+sha1b) );

		// Compare TigerTree URNs (true/false)
		assertEquals( Boolean.TRUE, Bitprint.getUriEquivalence("urn:tree:tiger:"+tta, "urn:tree:tiger:"+tta) );
		assertEquals( Boolean.FALSE, Bitprint.getUriEquivalence("urn:tree:tiger:"+tta, "urn:tree:tiger:"+ttb) );

		// Compare Bitprint URNs (true/false)
		assertEquals( Boolean.TRUE, Bitprint.getUriEquivalence("urn:bitprint:"+sha1a+"."+tta, "urn:bitprint:"+sha1a+"."+tta) );
		assertEquals( Boolean.FALSE, Bitprint.getUriEquivalence("urn:bitprint:"+sha1a+"."+tta, "urn:bitprint:"+sha1a+"."+ttb) );
		assertEquals( Boolean.FALSE, Bitprint.getUriEquivalence("urn:bitprint:"+sha1a+"."+tta, "urn:bitprint:"+sha1b+"."+tta) );
		assertEquals( Boolean.FALSE, Bitprint.getUriEquivalence("urn:bitprint:"+sha1a+"."+tta, "urn:bitprint:"+sha1b+"."+ttb) );

		// Compare Bitprint and SHA-1/TigerTree URNs (true/false)
		assertEquals( Boolean.TRUE, Bitprint.getUriEquivalence("urn:bitprint:"+sha1a+"."+tta, "urn:sha1:"+sha1a) );
		assertEquals( Boolean.FALSE, Bitprint.getUriEquivalence("urn:bitprint:"+sha1a+"."+tta, "urn:sha1:"+sha1b) );
		assertEquals( Boolean.TRUE, Bitprint.getUriEquivalence("urn:bitprint:"+sha1a+"."+tta, "urn:tree:tiger:"+tta) );
		assertEquals( Boolean.FALSE, Bitprint.getUriEquivalence("urn:bitprint:"+sha1a+"."+tta, "urn:tree:tiger:"+ttb) );
		
		// Compare SHA-1 and TigerTree URNs (null)
		assertNull( Bitprint.getUriEquivalence("urn:sha1:"+sha1a, "urn:tree:tiger:"+tta) );
		assertNull( Bitprint.getUriEquivalence("urn:tree:tiger:"+tta, "urn:sha1:"+sha1a) );
		
		// Compare other schemes (null)
		assertNull( Bitprint.getUriEquivalence("urn:tree:tiger:"+tta, "http://www.nuke24.net/") );
		assertNull( Bitprint.getUriEquivalence("urn:sha1:"+sha1a, "http://www.nuke24.net/") );
		assertNull( Bitprint.getUriEquivalence("http://slashdot.org/", "http://www.nuke24.net/") );
	}
}
