package contentcouch.http;

import contentcouch.blob.BlobUtil;
import contentcouch.store.Getter;
import contentcouch.value.Blob;
import contentcouch.value.Directory;
import junit.framework.TestCase;

public class HtmlDirectoryGetFilterTest extends TestCase {
	static String INDEX_STRING =
		"<html><head><title>Index</title></head><body>\n" +
		"<ul>\n" +
		"<li><a href=\"file.html\">file.html</a></li>\n" +
		"<li><a href=\"subdir/\">subdir</a></li>\n" +
		"<li><a href=\"http://www.nuke24.net/\">nuke24.net</a></li>\n" +
		"</ul>" +
		"</body></html>";
	
	Blob INDEX_BLOB = BlobUtil.getBlob(INDEX_STRING);
	
	public Getter getWhoopie() {
		return new Getter() {
			public Object get(String id) {
				if( "http://www.example.com/".equals(id) ) {
					return INDEX_BLOB;
				} else if( "http://www.example.com/file.html".equals(id) ) {
					return INDEX_BLOB;
				} else {
					return null;
				}
			}
		};
	}
	
	public Getter getWookie() {
		return new HtmlDirectoryGetFilter(getWhoopie());
	}
	
	
	public void testGetDirectory() {
		Object shouldBeDir = getWookie().get("http://www.example.com/");
		assertTrue( shouldBeDir instanceof Directory );
		assertEquals( 2, ((Directory)shouldBeDir).getEntries().size() ); 
	}

	public void testGetFile() {
		Object shouldBeFile = getWookie().get("http://www.example.com/file.html");
		assertTrue( shouldBeFile instanceof Blob ); 
	}
}
