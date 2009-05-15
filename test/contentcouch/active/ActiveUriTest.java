package contentcouch.active;

import java.util.Map;
import java.util.TreeMap;

import contentcouch.activefunctions.Hello;
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
	
	public void testParenExpressionParse() {
		ActiveUriResolver r = new ActiveUriResolver( null );
		Expression e = r.parseParenExpression("(foo bar=x:baz quux=x:quuux)");
		assertEquals("(foo bar=x:baz quux=x:quuux)", e.toString());
	}

	public void testParenExpressionParse2() {
		ActiveUriResolver r = new ActiveUriResolver( null );
		Expression e = r.parseParenExpression("((get-func name=data:,foofunc) bar=x:baz quux=x:quuux)");
		assertEquals("((get-func name=data:,foofunc) bar=x:baz quux=x:quuux)", e.toString());
	}
	
	/** Test that ResolveUriExpression sanitizes itself on toString() */
	public void testResolveUriSanitization() {
		Expression e = new ResolveUriExpression("http://www.nuke24.net/Biggs *(like a cemetary) ++/foobar");
		assertEquals("http://www.nuke24.net/Biggs%20%2A%28like%20a%20cemetary%29%20++/foobar", e.toString());
	}

	/** Test that active URIs get sanitized properly itself on toString() */
	public void testExpressionSanitization() {
		Map args = new TreeMap();
		args.put("bar%20* ", new ResolveUriExpression("http://example.org/(%20* )"));
		Expression e = new CallFunctionExpression( new GetFunctionByNameExpression("foo%20* "), args );
		assertEquals("(foo%2520%2A%20 bar%2520%2A%20=http://example.org/%28%20%2A%20%29)", e.toString());
	}

	/** Test that active URIs within (expressions) are parsed */
	public void testParenExpressionParse3() {
		ActiveUriResolver r = new ActiveUriResolver( null );
		Expression e = r.parseParenExpression("((get-func name=data:,foofunc) bar=x:baz quux=active:foo+bar@x:baz)");
		assertEquals("((get-func name=data:,foofunc) bar=x:baz quux=(foo bar=x:baz))", e.toString());
	}

	/** Test that active URIs as functions within (expressions) are parsed */
	public void testParenExpressionParse4() {
		ActiveUriResolver r = new ActiveUriResolver( null );
		Expression e = r.parseParenExpression("(active:get-func+name@data:,foofunc bar=x:baz quux=active:foo+bar@x:baz)");
		assertEquals("((get-func name=data:,foofunc) bar=x:baz quux=(foo bar=x:baz))", e.toString());
	}

	/** Test that (expressions) within active URIs are parsed */
	public void testParenExpressionInActiveUriParsed() {
		ActiveUriResolver r = new ActiveUriResolver( null );
		Expression e = r.parseActiveUriExpression("active:foo+bar@%28baz%20quux%3Dx%3Aqu%2Bux%29");
		assertEquals("(foo bar=(baz quux=x:qu+ux))", e.toString());
	}
	
	public void testGetFunctionByClassName() {
		Object f = new GetFunctionByNameExpression("contentcouch.hello").eval(new TreeMap());
		assertTrue("Got " + (f == null ? "null" : "a " + f.getClass()) + " but expected a Hello", f instanceof Hello);
		assertEquals("Hello, world", ((ActiveFunction)f).call(new TreeMap(), new TreeMap()));
	}
}
