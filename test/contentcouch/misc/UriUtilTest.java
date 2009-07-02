package contentcouch.misc;

import junit.framework.TestCase;

public class UriUtilTest extends TestCase {
	public void testEncode() {
		String encoded = "F%3A/blah";
		assertEquals("F:/blah", UriUtil.uriDecode(encoded));
	}
}
