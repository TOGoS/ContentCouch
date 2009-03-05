package contentcouch.path;

import junit.framework.TestCase;

public class PathUtilTest extends TestCase {
	public void testAppendPath1() {
		assertEquals("foo/bar", PathUtil.appendPath("foo/", "bar"));
	}
	public void testAppendPath2() {
		assertEquals("/bar", PathUtil.appendPath("foo/", "/bar"));
	}
	public void testAppendPath3() {
		assertEquals("E:/junk", PathUtil.appendPath("foo/", "E:/junk"));
	}
	public void testAppendPath4() {
		assertEquals("foo/bar", PathUtil.appendPath("foo/baz/", "../bar"));
	}
	public void testAppendPath5() {
		assertEquals("http://www.nuke24.net/style/",
				PathUtil.appendPath("http://www.nuke24.net/music/", "../style/"));
	}
	public void testAppendPath6() {
		// Handling of this kind of relative URI ain't implemented, yet.
		// Expect this test to fail.
		assertEquals("http://wwww.nuke24.net/",
				PathUtil.appendPath("http://www.nuke24.net/music/whatever", "/"));
	}
}
