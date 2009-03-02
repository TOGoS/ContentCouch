package contentcouch.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import junit.framework.TestCase;

public class ContentCouchRepositoryTest extends TestCase {
	public void testUnescape() {
		assertEquals("backslash\\doublequote\"newline\nreturn\rtab\t",
				ContentCouchRepository.unescape("backslash\\\\doublequote\\\"newline\\nreturn\\rtab\\t"));
	}
	
	public void testConfigLoad() throws IOException {
		ContentCouchRepository r = new ContentCouchRepository();
		r._loadConfig(new BufferedReader(new StringReader("[checkout] -link -shrink [store] -relink")), "test config");
		
		assertEquals("-link", ((List)r.cmdArgs.get("checkout")).get(0) );
		assertEquals("-shrink", ((List)r.cmdArgs.get("checkout")).get(1) );
		assertEquals("-relink", ((List)r.cmdArgs.get("store")).get(0) );
	}
}
