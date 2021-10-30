package contentcouch.misc;

import junit.framework.TestCase;
import contentcouch.framework.TheGetter;
import contentcouch.repository.MetaRepoConfig;

public class MemTempTest extends TestCase {
	public void setUp() {
		TheGetter.globalInstance = new MetaRepoConfig().getRequestKernel();
	}
	
	protected Object put(String uri, Object obj) {
		return TheGetter.put(uri, obj);
	}
	
	protected Object get(String uri) {
		return TheGetter.get(uri);
	}
	
	public void testPutStuff() {
		put("x-memtemp:/foobar", "ABC");
		assertEquals("ABC", get("x-memtemp:/foobar"));

		put("x-memtemp:/garf", get("x-memtemp:/"));
		assertEquals("ABC", get("x-memtemp:/garf/foobar"));
	}
}
