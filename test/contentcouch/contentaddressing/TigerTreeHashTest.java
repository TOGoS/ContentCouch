package contentcouch.contentaddressing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;

import org.bitpedia.util.Base32;
import org.bitpedia.util.Tiger;
import org.bitpedia.util.TigerTree;

import contentcouch.misc.ValueUtil;

/**
 * This is here more to demonstrate how you can use TigerTree hashes
 * more than to actually test any given implementation of it.
 * If you store the intermediate node blobs, you can reassemble
 * the original blob without needing any metadata.
 * 
 * Steps to reassemble:
 * 1 Take TTH uri:        urn:tree:tiger:XYZ
 * 2 Treat as Tiger URI:  urn:tiger:XYZ
 * 3 Find the blob with that Tiger hash.
 * 4 If the blob's first byte is 0, then the remaining bytes are your data.
 * 5 If the first byte is 1, then the rest of the blob lists more TTHashes.
 * 6 Recurse (go back to (3) for each hash listed), concatenating all data
 *   from leaf nodes to get your data.
 */
public class TigerTreeHashTest extends TestCase
{
	protected byte[] shortData = ValueUtil.getBytes("Hello, world!");
	protected byte[] longData = new byte[60000];
	public TigerTreeHashTest() {
		Random r = new Random(System.currentTimeMillis());
		r.nextBytes(longData);
	}
	
	protected Map storedBlobs = null;
	
	protected String tigerToUri(byte[] hash) {
		return "urn:tiger:"+Base32.encode(hash);
	}
	
	protected String tigerTreeToUri(byte[] hash) {
		return "urn:tree:tiger:"+Base32.encode(hash);
	}
	
	protected byte[] getBitpediaTTH( byte[] data ) {
		return new TigerTree().digest(data);
	}
	
	protected int BLOCKSIZE = 1024;
	
	// Non-storing TTH functions //
	
	protected byte[] getCustomLeafTTH( byte[] data, int off, int len ) {
		Tiger t = new Tiger();
		t.update((byte)0);
		t.update(data,off,len);
		return t.digest();
	}
	protected byte[] getCustomNodeHash( byte[] childHash1, byte[] childHash2 ) {
		Tiger t = new Tiger();
		t.update((byte)1);
		t.update(childHash1);
		t.update(childHash2);
		return t.digest();
	}
	protected byte[] getCustomNodeTTH( List childHashes ) {
		while( childHashes.size() > 1 ) {
			List parentHashes = new ArrayList();
			int i=0;
			while( i<childHashes.size()-1 ) {
				parentHashes.add( getCustomNodeHash((byte[])childHashes.get(i),(byte[])childHashes.get(i+1)) );
				i += 2;
			}
			if( i == childHashes.size()-1 ) {
				parentHashes.add(childHashes.get(i));
			}
			childHashes = parentHashes;
		}
		return (byte[])childHashes.get(0);
	}
	protected byte[] getCustomTTH( byte[] data ) {
		if( data.length < 1024 ) {
			return getCustomLeafTTH( data, 0, data.length );
		} else {
			List hashes = new ArrayList();
			for( int p=0; p<data.length; p+=BLOCKSIZE ) {
				int blockSize;
				if( data.length-p < BLOCKSIZE ) {
					blockSize = data.length-p;
				} else {
					blockSize = BLOCKSIZE;
				}
				hashes.add( getCustomLeafTTH(data,p,blockSize) );
			}
			return getCustomNodeTTH(hashes);
		}
	}
	
	// Storing TTH functions //
	
	protected void copy( byte[] src, int srcOff, byte[] dest, int destOff, int len ) {
		for( int i=0; i<len; ++i ) {
			dest[destOff+i] = src[srcOff+i];
		}
	}
	protected byte[] storeCustom( byte[] data ) {
		byte[] hash = new Tiger().digest(data);
		storedBlobs.put(tigerToUri(hash), data);
		return hash;
	}
	protected byte[] storeCustomLeaf( byte[] data, int off, int len ) {
		byte[] leafBlob = new byte[len+1];
		leafBlob[0] = 0;
		copy( data, off, leafBlob, 1, len );
		return storeCustom( leafBlob );
	}
	protected byte[] storeCustomNode( byte[] childHash1, byte[] childHash2 ) {
		byte[] nodeBlob = new byte[1+childHash1.length+childHash2.length];
		nodeBlob[0] = 1;
		copy( childHash1, 0, nodeBlob, 1, childHash1.length );
		copy( childHash2, 0, nodeBlob, 1+childHash1.length, childHash2.length );
		return storeCustom( nodeBlob );
	}
	protected byte[] storeCustomNode( List childHashes ) {
		while( childHashes.size() > 1 ) {
			List parentHashes = new ArrayList();
			int i=0;
			while( i<childHashes.size()-1 ) {
				parentHashes.add( storeCustomNode((byte[])childHashes.get(i),(byte[])childHashes.get(i+1)) );
				i += 2;
			}
			if( i == childHashes.size()-1 ) {
				parentHashes.add(childHashes.get(i));
			}
			childHashes = parentHashes;
		}
		return (byte[])childHashes.get(0);
	}
	protected byte[] storeCustomBlob( byte[] data ) {
		if( data.length < 1024 ) {
			// a dumb shortcut for small blobs
			return storeCustomLeaf( data, 0, data.length );
		} else {
			List hashes = new ArrayList();
			for( int p=0; p<data.length; p+=BLOCKSIZE ) {
				int blockSize;
				if( data.length-p < BLOCKSIZE ) {
					blockSize = data.length-p;
				} else {
					blockSize = BLOCKSIZE;
				}
				hashes.add( storeCustomLeaf(data,p,blockSize) );
			}
			return storeCustomNode(hashes);
		}
	}

	public void setUp() {
		storedBlobs = new HashMap();
	}
	
	public void testShortDataTigerTreeMatches() {
		byte[] customTTH = getCustomTTH(shortData);
		byte[] bitpediaTTH = getBitpediaTTH(shortData);
		assertEquals(tigerTreeToUri(bitpediaTTH), tigerTreeToUri(customTTH));
	}

	public void testLongDataTigerTreeMatches() {
		byte[] customTTH = getCustomTTH(longData);
		byte[] bitpediaTTH = getBitpediaTTH(longData);
		assertEquals(tigerTreeToUri(bitpediaTTH), tigerTreeToUri(customTTH));
	}
	
	public void testStoredShortDataTigerTreeMatches() {
		byte[] customTTH = storeCustomBlob(shortData);
		byte[] bitpediaTTH = getBitpediaTTH(shortData);
		assertEquals(tigerTreeToUri(bitpediaTTH), tigerTreeToUri(customTTH));
	}

	public void testStoredLongDataTigerTreeMatches() {
		byte[] customTTH = storeCustomBlob(longData);
		byte[] bitpediaTTH = getBitpediaTTH(longData);
		assertEquals(tigerTreeToUri(bitpediaTTH), tigerTreeToUri(customTTH));
	}

	protected void writeLeaves( String uri, OutputStream os ) throws IOException {
		byte[] blob = (byte[])storedBlobs.get(uri);
		if( blob[0] == 0 ) {
			os.write( blob, 1, blob.length-1 );
		} else {
			for( int i=1; i<blob.length; i+=24 ) {
				byte[] h = new byte[24];
				copy( blob, i, h, 0, 24 );
				writeLeaves( tigerToUri(h), os );
			}
		}
	}
	
	public void testReassembleLongData() throws IOException {
		byte[] longDataTTH = storeCustomBlob(longData);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writeLeaves( tigerToUri(longDataTTH), baos );
		byte[] reassembled = baos.toByteArray();
		
		assertEquals( tigerTreeToUri(getBitpediaTTH(longData)), tigerTreeToUri(getBitpediaTTH(reassembled)) );
	}
}
