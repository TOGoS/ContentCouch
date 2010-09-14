package contentcouch.path;

import contentcouch.active.ActiveUtil;
import contentcouch.framework.TheGetter;
import contentcouch.misc.UriUtil;
import contentcouch.repository.MetaRepoConfig;
import junit.framework.TestCase;

public class PathUtilTest extends TestCase {
	public void setUp() {
		TheGetter.globalInstance = new MetaRepoConfig().getRequestKernel();
	}
	
	public void testAppendPath1() {
		assertEquals("foo/bar", PathUtil.appendPath("foo/", "bar"));
	}
	public void testAppendPath2() {
		assertEquals("/bar", PathUtil.appendPath("foo/", "/bar"));
	}
	public void testAppendPath3() {
		// This should work because E will be interpreted as a URI scheme!
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
		assertEquals("http://www.nuke24.net/",
				PathUtil.appendPath("http://www.nuke24.net/music/whatever", "/"));
	}
	public void testAppendPath7() {
		assertEquals("foo/bar", PathUtil.appendPath("foo", "bar", false));
	}
	public void testAppendPath8() {
		assertEquals("foo/bar/", PathUtil.appendPath("foo", "bar/", false));
	}
	
	public void testIdentifyAbsolutePaths() {
		assertTrue( PathUtil.isAbsolute("file:/gopher") );
		assertTrue( PathUtil.isAbsolute("E:/gopher") );
		assertTrue( PathUtil.isAbsolute("/gopher") );
		assertTrue( PathUtil.isAbsolute("http://gopher/food") );
		assertTrue( PathUtil.isAbsolute("file:gopher/food") );
		
		assertFalse( PathUtil.isAbsolute("gopher/food") );
	}
	
	public void testIdentifyHierarchicalUris() {
		assertTrue( PathUtil.isHierarchicalUri("file:poopyscoopy") );
		assertTrue( PathUtil.isHierarchicalUri("file:/poopyscoopy") );
		assertTrue( PathUtil.isHierarchicalUri("file://poopy/scoopy") );
		assertTrue( PathUtil.isHierarchicalUri("marg:/poopy/scoopy") );
		assertTrue( PathUtil.isHierarchicalUri("http:/poopy/scoopy") );
		assertFalse( PathUtil.isHierarchicalUri("marg:poopy/scoopy") );
		assertFalse( PathUtil.isHierarchicalUri("urn:sha1:POOPSKOOP") );
		assertFalse( PathUtil.isHierarchicalUri("data:,POOPSKOOP") );
	}
	
	public void testAppendFollowPath() {
		assertEquals("http://www.nuke24.net/images/bunny.png", PathUtil.appendPath("active:contentcouch.follow-path+source@http://www.nuke24.net/+path@data:,images/", "bunny.png"));
	}

	public void testReplaceFollowPath() {
		assertEquals("http://slashdot.org/", PathUtil.appendPath("active:contentcouch.follow-path+source@http://www.nuke24.net/+path@data:,images/", "http://slashdot.org/"));
	}
	
	public void testExpressionPathFollowing() {
		assertEquals("active:contentcouch.directoryize+operand@" + UriUtil.uriEncode("http://nuke24.net/images/bunny.jpg"),
				PathUtil.appendPath("active:contentcouch.directoryize+operand@" + UriUtil.uriEncode("http://nuke24.net/images/"), "bunny.jpg"));
	}

	public void testPathFollowingSimplifiesSubExpressions() {
		assertEquals(
			"active:contentcouch.directoryize+operand@" + UriUtil.uriEncode( "http://www.nuke24.net/images/bunny.jpg"),
			PathUtil.appendPath(
				"active:contentcouch.directoryize+operand@" + UriUtil.uriEncode(
					"active:contentcouch.follow-path+source@http://www.nuke24.net/+path@data:,images/"),
				"bunny.jpg"
			)
		);

		assertEquals(
			"http://www.nuke24.net/images/bunny.jpg",
			ActiveUtil.simplify(
				"active:contentcouch.follow-path+source@" + UriUtil.uriEncode(
					"active:contentcouch.follow-path+source@http://www.nuke24.net/+path@data:,images/") + "+" +
				"path@data:,bunny.jpg"
			)
		);

		assertEquals(
			"http://www.nuke24.net/style/green.css",
			ActiveUtil.simplify(
				"active:contentcouch.follow-path+source@" + UriUtil.uriEncode(
					"active:contentcouch.follow-path+source@http://www.nuke24.net/+path@data:,images/") + "+" +
				"path@data:,../style/green.css"
			)
		);

		assertEquals(
			"http://www.nuke24.net/style/green.css",
			ActiveUtil.simplify(
				"active:contentcouch.follow-path+source@" + UriUtil.uriEncode(
					"active:contentcouch.follow-path+source@http://www.nuke24.net/+path@data:,images/") + "+" +
				"path@data:,/style/green.css"
			)
		);

		assertEquals(
			"active:contentcouch.directoryize+operand@" + UriUtil.uriEncode("http://www.nuke24.net/images/bunny.jpg"),
			ActiveUtil.simplify(
				"active:contentcouch.follow-path+source@" + UriUtil.uriEncode(
					"active:contentcouch.directoryize+operand@" + UriUtil.uriEncode(
						"active:contentcouch.follow-path+source@http://www.nuke24.net/+path@data:,images/")) + "+" +
				"path@data:,bunny.jpg"
			)
		);
	}
}
