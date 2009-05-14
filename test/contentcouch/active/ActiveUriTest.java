package contentcouch.active;

import contentcouch.misc.UriUtil;
import junit.framework.TestCase;

public class ActiveUriTest extends TestCase {
	public void testResolveUriExpressionToString() {
		assertEquals("http://www.nuke24.net/", new ResolveUriExpression("http://www.nuke24.net/").toString());
	}
	
	public void testUriEncode() {
		assertEquals("ABC%20%2B%20123", UriUtil.uriEncode("ABC + 123"));
	}

	public void testUriDecode() {
		assertEquals("ABC + 123", UriUtil.uriDecode("ABC%20%2B%20123"));
		assertEquals("ABC + 123", UriUtil.uriDecode("ABC%20%2b%20123"));
	}

	public void testActiveUriParse() {
		ActiveUriResolver r = new ActiveUriResolver( null );
		Expression e = r.parseExpression("active:foo+bar@x:baz+quux@x:quuux");
		assertEquals("(foo bar=x:baz quux=x:quuux)", e.toString());
	}

	public void testActiveUriParse2() {
		ActiveUriResolver r = new ActiveUriResolver( null );
		Expression e = r.parseExpression("active:active:get-func%2bname%40data:,foofunc+bar@x:baz+quux@x:quuux");
		assertEquals("((get-func name=data:,foofunc) bar=x:baz quux=x:quuux)", e.toString());
	}
}
