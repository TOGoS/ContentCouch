package contentcouch.directory;

import java.util.Collections;
import java.util.Map;

import junit.framework.TestCase;
import togos.mf.value.Blob;
import contentcouch.blob.BlobUtil;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.value.Directory;

public abstract class WritableDirectoryTest extends TestCase {
	protected WritableDirectory dir;
	
	static Blob hwblob = BlobUtil.getBlob("Hello, world!");
	Map options = Collections.EMPTY_MAP;
	
	static SimpleDirectory subdir;
	static {
		subdir = new SimpleDirectory();
		SimpleDirectory.Entry e = new SimpleDirectory.Entry();
		e.name = "rox";
		e.lastModified = 12345;
		e.targetSize = hwblob.getLength();
		e.targetType = CCouchNamespace.TT_SHORTHAND_BLOB;
		e.target = hwblob;
		subdir.addDirectoryEntry(e);
	}
	
	public void testAddEntry() {
		SimpleDirectory.Entry e = new SimpleDirectory.Entry();
		e.name = "rox";
		e.lastModified = 12345;
		e.targetSize = hwblob.getLength();
		e.targetType = CCouchNamespace.TT_SHORTHAND_BLOB;
		e.target = hwblob;
		dir.addDirectoryEntry(e, Collections.EMPTY_MAP);
		
		Directory.Entry e2 = dir.getDirectoryEntry("rox");
		assertNotNull(e2);
		assertEquals("rox",e2.getName());
		assertEquals(e.targetSize, e2.getTargetSize());
		assertEquals(e.targetType, e2.getTargetType());
		assertEquals(e.lastModified, e2.getLastModified());
	}
	
	public void testDeleteEntry() {
		SimpleDirectory.Entry e = new SimpleDirectory.Entry();
		e.name = "rox";
		e.lastModified = 12345;
		e.targetSize = hwblob.getLength();
		e.targetType = CCouchNamespace.TT_SHORTHAND_BLOB;
		e.target = hwblob;
		dir.addDirectoryEntry(e, options);
		
		dir.deleteDirectoryEntry("rox", options);
		
		Directory.Entry e2 = dir.getDirectoryEntry("rox");
		assertNull( e2 );
	}

	public void testAddSubdirEntry() {
		SimpleDirectory.Entry e = new SimpleDirectory.Entry();
		e.name = "rox";
		e.targetType = CCouchNamespace.TT_SHORTHAND_DIRECTORY;
		e.target = subdir;
		dir.addDirectoryEntry(e, Collections.EMPTY_MAP);
		
		Directory.Entry e2 = dir.getDirectoryEntry("rox");
		assertNotNull(e2);
		assertEquals("rox",e2.getName());
		assertEquals(e.targetType, e2.getTargetType());
	}
	
	public void testDeleteSubdirEntry() {
		SimpleDirectory.Entry e = new SimpleDirectory.Entry();
		e.name = "rox";
		e.targetType = CCouchNamespace.TT_SHORTHAND_DIRECTORY;
		e.target = subdir;
		dir.addDirectoryEntry(e, options);
		
		dir.deleteDirectoryEntry("rox", options);
		
		Directory.Entry e2 = dir.getDirectoryEntry("rox");
		assertNull( e2 );
	}
}
