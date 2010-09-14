package contentcouch.activefunctions;

import togos.mf.value.Blob;
import contentcouch.blob.BlobUtil;
import contentcouch.blob.ByteArrayBlob;
import contentcouch.framework.TheGetter;
import contentcouch.misc.UriUtil;
import contentcouch.repository.MetaRepoConfig;
import junit.framework.TestCase;

public class ConcatTest extends TestCase {
	public void setUp() {
		TheGetter.globalInstance = new MetaRepoConfig().getRequestKernel();
	}
	
	public void testConcat() {
		byte[] b1 = new byte[] { (byte)0xFF, 0x00, (byte)0xEE, 0x11 };
		byte[] b2 = new byte[] { 0x55, (byte)0xCC, 0x33, (byte)0xDD };
		byte[] b3 = new byte[] { (byte)0xFF, 0x00, (byte)0xEE, 0x11, 0x55, (byte)0xCC, 0x33, (byte)0xDD };
		String uri = "active:contentcouch.concat+operand@" + UriUtil.uriEncode(UriUtil.makeDataUri(b1)) + "+operand1@" + UriUtil.uriEncode(UriUtil.makeDataUri(b2));
		Blob b = (Blob)TheGetter.get(uri);
		assertEquals( 0, BlobUtil.compareBlobs(b, new ByteArrayBlob(b3)));
	}
}
