package contentcouch.misc;

import junit.framework.TestCase;

public class UriUtilTest extends TestCase {
	public void testEncode() {
		String encoded = "F%3A/blah";
		assertEquals("F:/blah", UriUtil.uriDecode(encoded));
	}
	
	public void testSanitize() {
		assertEquals("http://www.nuke24.net/", UriUtil.sanitizeUri("http://www.nuke24.net/"));
	}
	
	public void testSanitize2() {
		assertEquals("http://www.nuke24.net/Biggs%20*(like%20a%20cemetary)%20++/foobar",
			UriUtil.sanitizeUri("http://www.nuke24.net/Biggs *(like a cemetary) ++/foobar"));
	}
	
	public void testStripRdfPrefix() {
		assertEquals("urn:sha1:FOOBAR", UriUtil.stripRdfSubjectPrefix("x-parse-rdf:urn:sha1:FOOBAR"));
		assertEquals("urn:sha1:FOOBAR", UriUtil.stripRdfSubjectPrefix("x-rdf-subject:urn:sha1:FOOBAR"));
		assertNull(UriUtil.stripRdfSubjectPrefix("urn:sha1:FOOBAR"));
	}
	
	public void testIsPureContentUri() {
		assertTrue( UriUtil.isPureContentUri("x-rdf-subject:urn:bitprint:LOYEC6PAEGZWELTEDM6IVARRAZVKVF53.4Q5BHWSIYAAY6DQ2JBKZ5QNGKYPNGVAWHJLIA5I") );
		assertTrue( UriUtil.isPureContentUri("x-parse-rdf:urn:bitprint:LOYEC6PAEGZWELTEDM6IVARRAZVKVF53.4Q5BHWSIYAAY6DQ2JBKZ5QNGKYPNGVAWHJLIA5I") );
		assertTrue( UriUtil.isPureContentUri("urn:bitprint:LOYEC6PAEGZWELTEDM6IVARRAZVKVF53.4Q5BHWSIYAAY6DQ2JBKZ5QNGKYPNGVAWHJLIA5I") );
		assertTrue( UriUtil.isPureContentUri("x-parse-rdf:urn:sha1:LOYEC6PAEGZWELTEDM6IVARRAZVKVF53") );
		assertTrue( UriUtil.isPureContentUri("urn:tree:tiger:4Q5BHWSIYAAY6DQ2JBKZ5QNGKYPNGVAWHJLIA5I") );
		assertFalse( UriUtil.isPureContentUri("http://www.nuke24.net/") );
		assertFalse( UriUtil.isPureContentUri("x-rdf-subject:x-ccouch-head:togos-win/archives/images/TOGoS/latest") );
		assertFalse( UriUtil.isPureContentUri("x-ccouch-head:togos-win/archives/images/TOGoS/latest") );
		assertFalse( UriUtil.isPureContentUri("3r34nfiu34nf3$R@#E!@3e") );
	}
}
