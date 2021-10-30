package contentcouch.misc;

import junit.framework.TestCase;

public class SplitTest extends TestCase {
	public void testSplit() {
		assertEquals(3, "foo/bar//baz///".split("/+").length);
		assertEquals(1, "".split("/+").length);
	}
}
