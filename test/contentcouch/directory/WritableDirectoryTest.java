package contentcouch.directory;

import java.util.Collections;
import java.util.Map;

import junit.framework.TestCase;
import togos.mf.value.Blob;
import contentcouch.blob.BlobUtil;
import contentcouch.value.Directory;

public abstract class WritableDirectoryTest extends TestCase {
	protected WritableDirectory dir;
	
	Blob hwblob = BlobUtil.getBlob("Hello, world!");
	Map options = Collections.EMPTY_MAP;
	
	public void testAddEntry() {
		SimpleDirectory.Entry e = new SimpleDirectory.Entry();
		e.name = "rox";
		e.targetLastModified = 12345;
		e.targetSize = hwblob.getLength();
		e.target = hwblob;
		dir.addDirectoryEntry(e, Collections.EMPTY_MAP);
		
		Directory.Entry e2 = dir.getDirectoryEntry("rox");
		assertNotNull(e2);
		assertEquals("rox",e2.getName());
		assertEquals(e.targetSize, e2.getTargetSize());
		assertEquals(e.targetLastModified, e2.getLastModified());
	}
	
	public void testDeleteEntry() {
		SimpleDirectory.Entry e = new SimpleDirectory.Entry();
		e.name = "rox";
		e.targetLastModified = 12345;
		e.targetSize = hwblob.getLength();
		e.target = hwblob;
		dir.addDirectoryEntry(e, options);
		
		dir.deleteDirectoryEntry("rox", options);
		
		Directory.Entry e2 = dir.getDirectoryEntry("rox");
		assertNull( e2 );
	}
}
