package contentcouch.directory;

import java.util.Iterator;

import junit.framework.TestCase;
import contentcouch.repository.MetaRepoConfig;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;

public class DirectoryWalkerTest extends TestCase {
	Directory d;
	
	public void setUp() {
		TheGetter.globalInstance = new MetaRepoConfig().getRequestKernel();
		d = TheGetter.getDirectory(
			"(contentcouch.create-directory\n" +
			"  entries/a=(contentcouch.create-directory entries/ax=\"ax\")\n" +
			"  entries/b=(contentcouch.create-directory entries/bx=\"bx\")\n" +
			"  entries/d=(contentcouch.create-directory entries/dx=\"dx\")\n" +
			"  entries/c=(contentcouch.create-directory entries/cx=\"cx\")\n" +
			")");
	}
	
	
	public void testDirectoryWalk() {
		Iterator i = new DirectoryWalker(d, false);
		assertEquals("a",  ((Directory.Entry)i.next()).getName() );
		assertEquals("ax", ((Directory.Entry)i.next()).getName() );
		assertEquals("b",  ((Directory.Entry)i.next()).getName() );
		assertEquals("bx", ((Directory.Entry)i.next()).getName() );
		assertEquals("c",  ((Directory.Entry)i.next()).getName() );
		assertEquals("cx", ((Directory.Entry)i.next()).getName() );
		assertEquals("d",  ((Directory.Entry)i.next()).getName() );
		assertEquals("dx", ((Directory.Entry)i.next()).getName() );
	}
}
