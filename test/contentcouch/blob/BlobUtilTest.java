package contentcouch.blob;

import junit.framework.TestCase;

public class BlobUtilTest extends TestCase {
	public void testCompareBlobs() {
		assertEquals(0, BlobUtil.compareBlobs(
				BlobUtil.getBlob("HELLO"),
				BlobUtil.getBlob("HELLO")
		));

		assertEquals(-1, BlobUtil.compareBlobs(
				BlobUtil.getBlob("AELLO"),
				BlobUtil.getBlob("HELLO")
		));

		assertEquals(1, BlobUtil.compareBlobs(
				BlobUtil.getBlob("HELLO"),
				BlobUtil.getBlob("AELLO")
		));

		assertEquals(1, BlobUtil.compareBlobs(
				BlobUtil.getBlob("HELLOA"),
				BlobUtil.getBlob("HELLO")
		));
		
		assertEquals(-1, BlobUtil.compareBlobs(
				BlobUtil.getBlob("HELLO"),
				BlobUtil.getBlob("HELLOA")
		));
	}
}
