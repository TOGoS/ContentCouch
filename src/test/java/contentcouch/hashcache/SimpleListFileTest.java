package contentcouch.hashcache;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;
import contentcouch.file.FileUtil;

public class SimpleListFileTest extends TestCase
{
	Random r = new Random();
	
	protected byte[] rand( int maxLen ) {
		int length = r.nextInt(maxLen);
		byte[] data = new byte[length];
		r.nextBytes(data);
		return data;
	}
	
	File f = new File("junk/slf-test.slf2");
	
	public void setUp() throws IOException {
		if( f.exists() ) f.delete();
		else FileUtil.mkParentDirs(f);
	}
	
	public void tearDown() throws IOException {
		f.deleteOnExit();
	}
	
	protected boolean equals( byte[] b1, byte[] b2 ) {
		if( b1 == b2 ) return true;
		if( b1 == null || b2 == null ) return false;
		if( b1.length != b2.length ) return false;
		
		for( int i=0; i<b1.length; ++i ) {
			if( b2[i] != b1[i] ) return false;
		}
		return true;
	}
	
	protected void _testReadWrite( boolean lock ) throws IOException {
		if( f.exists() ) f.delete();
		
		SimpleListFile slf = new SimpleListFile(f, "rw");
		slf.initIfEmpty( 256, 65536 );
		slf.autoLockingEnabled = lock;
		
		HashMap kv = new HashMap();
		
		for( int i=0; i<1024; ++i ) {
			byte[] k = new byte[]{}; //rand( 128 );
			byte[] v = rand( 512 );
			kv.put( k, v );
			slf.put( k, v );
			byte[] got = slf.get(k);
			if( got == null ) {
				fail("Couldn't retrieve key of length "+k.length+", hashcode = "+SimpleListFile.hash(k)+", index index = "+slf.indexIndex(k));
			} else if( !equals( v, got ) ) {
				fail("Value didn't match for key of length "+k.length+", hashcode = "+SimpleListFile.hash(k)+", index index = "+slf.indexIndex(k));
			}
			System.err.println("OKA");
		}
		
		for( Iterator i=kv.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			assertTrue(equals( (byte[])e.getValue(), slf.get((byte[])e.getKey()) ));
		}
		
		slf.close();
		
		slf = new SimpleListFile(f, "rw");
		slf.autoLockingEnabled = lock;
		
		for( Iterator i=kv.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			assertTrue(equals( (byte[])e.getValue(), slf.get((byte[])e.getKey()) ));
		}
	}
	
	public void testReadWrite() throws IOException {
		_testReadWrite( false );
	}
	
	public void testReadWriteWithLocks() throws IOException {
		_testReadWrite( true );
	}
}
