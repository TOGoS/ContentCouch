package contentcouch.http;

import togos.rra.BaseRequestHandler;
import togos.rra.BaseResponse;
import togos.rra.Getter;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.blob.BlobUtil;
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
	
	public BaseRequestHandler getWhoopie() {
		return new BaseRequestHandler() {
			public Response handleRequest(Request req) {
				if( "http://www.example.com/".equals(req.getUri()) ) {
					return new BaseResponse(Response.STATUS_NORMAL, INDEX_BLOB);
				} else if( "http://www.example.com/file.html".equals(req.getUri()) ) {
					return new BaseResponse(Response.STATUS_NORMAL, INDEX_BLOB);
				} else {
					return BaseResponse.RESPONSE_UNHANDLED;
				}
			}
		};
	}
	
	public Getter getWookie() {
		return new HtmlDirectoryResponseFilter(getWhoopie());
	}
	
	
	public void testGetDirectory() {
		Object shouldBeDir = getWookie().get("http://www.example.com/");
		assertTrue( shouldBeDir instanceof Directory );
		assertEquals( 2, ((Directory)shouldBeDir).getDirectoryEntrySet().size() ); 
	}

	public void testGetFile() {
		Object shouldBeFile = getWookie().get("http://www.example.com/file.html");
		assertTrue( shouldBeFile instanceof Blob ); 
	}
}
