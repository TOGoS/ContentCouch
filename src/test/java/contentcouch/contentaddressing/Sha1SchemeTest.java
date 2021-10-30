package contentcouch.contentaddressing;

import junit.framework.TestCase;
import contentcouch.blob.BlobUtil;
import contentcouch.misc.ValueUtil;

public class Sha1SchemeTest extends TestCase {
	byte[] bytesToHash = ValueUtil.getBytes("Hallo, werlde!");
	byte[] expectedHash = new byte[] {
		0x4b, 0x0b, 0x58, 0x19, 0x42, 0x0d, (byte)0xf6, 0x60,
		0x48, 0x0c, 0x31, (byte)0xa8, 0x57, (byte)0xad, 0x14,
		(byte)0xac, 0x6c, 0x42, (byte)0x8c, 0x2a
	};
	String expectedUrn = "urn:sha1:JMFVQGKCBX3GASAMGGUFPLIUVRWEFDBK";
	String expectedFilename = "JMFVQGKCBX3GASAMGGUFPLIUVRWEFDBK";
	String expectedRdfValue = "JMFVQGKCBX3GASAMGGUFPLIUVRWEFDBK";
	
	public void testHash() {
		assertTrue( Sha1Scheme.getInstance().couldTranslateUrn("urn:sha1:JMFVQGKCBX3GASAMGGUFPLIUVRWEFDBK") );
		assertTrue( Sha1Scheme.getInstance().couldTranslateUrn("urn:bitprint:JMFVQGKCBX3GASAMGGUFPLIUVRWEFDBK.QXGGY") );
		assertFalse( Sha1Scheme.getInstance().couldTranslateUrn("http://zombo.com/") );
		
		byte[] hash = Sha1Scheme.getInstance().getHash( BlobUtil.getBlob(bytesToHash) );
		assertEquals( expectedUrn, Sha1Scheme.getInstance().hashToUrn(hash) );
		assertEquals( expectedFilename, Sha1Scheme.getInstance().hashToFilename(hash) );
		assertEquals( expectedRdfValue, Sha1Scheme.getInstance().hashToRdfValue(hash) );
	}
}
