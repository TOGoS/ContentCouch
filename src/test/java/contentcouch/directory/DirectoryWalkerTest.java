package contentcouch.directory;

import java.util.Iterator;

import junit.framework.TestCase;
import contentcouch.blob.BlobUtil;
import contentcouch.framework.TheGetter;
import contentcouch.misc.Function1;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.repository.MetaRepoConfig;
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
		assertTrue( i.hasNext() );
		assertEquals("a",  ((Directory.Entry)i.next()).getName() );
		assertTrue( i.hasNext() );
		assertEquals("ax", ((Directory.Entry)i.next()).getName() );
		assertTrue( i.hasNext() );
		assertEquals("b",  ((Directory.Entry)i.next()).getName() );
		assertTrue( i.hasNext() );
		assertEquals("bx", ((Directory.Entry)i.next()).getName() );
		assertTrue( i.hasNext() );
		assertEquals("c",  ((Directory.Entry)i.next()).getName() );
		assertTrue( i.hasNext() );
		assertEquals("cx", ((Directory.Entry)i.next()).getName() );
		assertTrue( i.hasNext() );
		assertEquals("d",  ((Directory.Entry)i.next()).getName() );
		assertTrue( i.hasNext() );
		assertEquals("dx", ((Directory.Entry)i.next()).getName() );
		assertFalse( i.hasNext() );
	}
	
	public void testBlobFilter() {
		Function1 bf = EntryFilters.BLOBFILTER;
		assertNull( bf.apply(new SimpleDirectory.Entry("x", new SimpleDirectory(), null)) );
		assertNotNull( bf.apply(new SimpleDirectory.Entry("x", BlobUtil.getBlob("x"), null)) );
		assertNotNull( bf.apply(new SimpleDirectory.Entry("x", new SimpleDirectory(), CCouchNamespace.TT_SHORTHAND_BLOB)) );
		assertNull( bf.apply(new SimpleDirectory.Entry("x", BlobUtil.getBlob("x"), CCouchNamespace.TT_SHORTHAND_DIRECTORY)) );
	}
	
	public void testBlobOnlyDirectoryWalk() {
		Iterator i = new FilterIterator( new DirectoryWalker(d, false), EntryFilters.BLOBFILTER );
		assertTrue( i.hasNext() );
		assertEquals("ax", ((Directory.Entry)i.next()).getName() );
		assertTrue( i.hasNext() );
		assertEquals("bx", ((Directory.Entry)i.next()).getName() );
		assertTrue( i.hasNext() );
		assertEquals("cx", ((Directory.Entry)i.next()).getName() );
		assertTrue( i.hasNext() );
		assertEquals("dx", ((Directory.Entry)i.next()).getName() );
		assertFalse( i.hasNext() );
	}
}
