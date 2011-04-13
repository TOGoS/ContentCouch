package contentcouch.active;

import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.base.BaseRequest;
import contentcouch.encoding.Base64;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import junit.framework.TestCase;

public class DataUriResolverTest extends TestCase {
	protected String[] testStrings = new String[] {
		"a",
		"data:12345not a real data uri hahaha",
		"wt&f=s&ux",
		"wt&f=s&ux?",
		"wt&f=s&ux?%20 %20 th!ng|&/\\sutff\""
	};
	
	protected DataUriResolver resolver = new DataUriResolver();

	protected void assertDecodesTo( String expectedValue, String dataUri ) {
		Response res = resolver.call(new BaseRequest(RequestVerbs.GET,dataUri));
		assertEquals( expectedValue, ValueUtil.getString(res.getContent()) );
	}
	
	protected void assertUriEncodingWorks( String encodeThis ) {
		assertDecodesTo( encodeThis, UriUtil.makeDataUri(encodeThis) );
	}
	
	protected void assertBase64EncodingWorks( String encodeThis ) {
		assertDecodesTo( encodeThis, "data:text/plain;base64,"+new String(Base64.encode(ValueUtil.getBytes(encodeThis))) );
	}

	public void testDecodeUriEncodedStuff() {
		for( int i=0; i<testStrings.length; ++i ) {
			assertUriEncodingWorks(testStrings[i]);
		}
	}

	public void testDecodeBase64EncodedStuff() {
		for( int i=0; i<testStrings.length; ++i ) {
			assertBase64EncodingWorks(testStrings[i]);
		}
	}

}
