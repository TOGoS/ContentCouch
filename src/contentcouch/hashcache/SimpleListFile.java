package contentcouch.hashcache;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileLock;
import java.util.Arrays;

import contentcouch.blob.Blob;



/** Format of this kind of file:
 * 
 * overall structure:
 *   [header]
 *   [index chunk]
 *   [data and reclaimable chunks]
 *   [end-of-file chunk]
 * 
 * header:
 *   [128 bytes of padding]
 * 
 * chunk:
 *   [4-byte offset of physical prev chunk]
 *   [4-byte offset of physical next chunk]
 *   [4-byte offset of list prev chunk]
 *   [4-byte offset of list next chunk]
 *   [4-byte chunk type]
 *   [chunk content]
 *   
 * chunk types:
 *   'RECL' - to be recycled
 *   'INDX' - index
 *   'LIST' - placeholder for first/last item in a list
 *   'PAIR' - dictionary item
 *   'ENDF' - end of file
 * 
 * index (index size + 16) * 4 bytes:
 *    0 [4-byte number of entries (not including the 15 reserved ones)]
 *    4 [offset of last chunk in file]    \
 *    8 [offset to recycle list]           } 15 reserved entries
 *   12 [13 more reserved entries]        /
 *   60 [offset to items]
 *
 * dictionary item chunk content:
 *   [4-byte length of name]
 *   [name]
 *   [4-byte length of data]
 *   [data]
 *   [4-byte 0]
 */
public class SimpleListFile {
	public static class Chunk {
		public int offset = 0;
		public int prevOffset = 0;
		public int nextOffset = 0;
		public int listPrevOffset = 0;
		public int listNextOffset = 0;
		public int type = 0;
	}
	
	protected static char[] hexChars = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	protected static String intToHex(int i) {
		return "0x" +
			hexChars[(i >> 28)&0xF] +
			hexChars[(i >> 24)&0xF] +
			hexChars[(i >> 20)&0xF] +
			hexChars[(i >> 16)&0xF] +
			hexChars[(i >> 12)&0xF] +
			hexChars[(i >>  8)&0xF] +
			hexChars[(i >>  4)&0xF] +
			hexChars[(i >>  0)&0xF];
	}
		
	
	public static int HEADER_LENGTH = 128;
	public static int CHUNK_HEADER_LENGTH = 4*6;
	public static int CHUNK_MAGIC_OFFSET = 0;
	public static int CHUNK_PREV_OFFSET = 4;
	public static int CHUNK_NEXT_OFFSET = 8;
	public static int CHUNK_LIST_PREV_OFFSET = 12;
	public static int CHUNK_LIST_NEXT_OFFSET = 16;
	public static int CHUNK_TYPE_OFFSET = 20;
	
	public static int INDEX_CHUNK_OFFSET = HEADER_LENGTH;
	public static int INDEX_SIZE_OFFSET = INDEX_CHUNK_OFFSET + CHUNK_HEADER_LENGTH;
	public static int INDEX_ITEMS_OFFSET = INDEX_CHUNK_OFFSET + CHUNK_HEADER_LENGTH + 4;
	public static int RESERVED_INDEX_ITEMS = 15;
	public static int ENDF_LIST_INDEX = 0;
	public static int RECL_LIST_INDEX = 1;
	public static int USER_INDEX_ITEMS_OFFSET = INDEX_ITEMS_OFFSET + RESERVED_INDEX_ITEMS*4;
	
	public static int CHUNK_MAGIC = strToInt("CHNK");
	
	public static int CHUNK_TYPE_RECL = strToInt("RECL");
	public static int CHUNK_TYPE_INDX = strToInt("INDX");
	public static int CHUNK_TYPE_LIST = strToInt("LIST");
	public static int CHUNK_TYPE_PAIR = strToInt("PAIR");
	public static int CHUNK_TYPE_ENDF = strToInt("ENDF");
	
	protected File file;
	protected RandomAccessFile raf;
	protected boolean writeMode;
	protected int indexSize;
	/**
	 * If true, high-level functions will automatically aquire a lock on the
	 * underlying file before reading and writing
	 * */
	protected boolean autoLockingEnabled = true;
	
	//// 'life cycle' functions ////
	
	public SimpleListFile(Blob blob, String mode) throws IOException {
		if( blob instanceof File ) {
			initr((File)blob, mode);
		} else {
			throw new RuntimeException("Can't open SimpleListFile except on files, sry");
		}
	}
	
	public SimpleListFile(File file, String mode) throws IOException {
		initr(file, mode);
	}
	
	public void clear() throws IOException {
		if( !writeMode ) throw new IOException("Can't clear unless in write mode");
		this.raf.setLength(0);
	}
	
	protected byte[] createIndexData(int numEntries) {
		byte[] dat = new byte[(numEntries+RESERVED_INDEX_ITEMS)*4 + 16*4];
		intToBytes(numEntries, dat, 0);
		return dat;
	}

	protected void initr(File file, String mode) throws IOException {
		this.file = file;
		this.raf = new RandomAccessFile(file, mode);
		writeMode = (mode.indexOf('w') != -1);
		
		if( raf.length() > HEADER_LENGTH ) {
			this.indexSize = getIntAt(INDEX_SIZE_OFFSET);
		}
	}

	public void initIfEmpty(int indexSize, int fileSize) throws IOException {
		if( this.raf.length() <= HEADER_LENGTH ) {
			this.indexSize = indexSize;
			if( !writeMode ) throw new IOException("Can't initialize unless in write mode");
			this.raf.setLength(fileSize);
			Chunk indexChunk = new Chunk();
			indexChunk.offset = INDEX_CHUNK_OFFSET;
			indexChunk.type = CHUNK_TYPE_INDX;
			writeChunk(indexChunk, createIndexData(indexSize));
			Chunk eofChunk = writeEofChunk(indexChunk.offset);
			setLastChunkOffset(eofChunk.offset);
		}
	}
	
	public void close() throws IOException {
		this.raf.close();
	}
	
	//// Accessors ////
	
	public File getFile() {
		return file;
	}
	
	public boolean isWritable() {
		return writeMode;
	}
	
	//// Int r/w functions ////
	
	static void intToBytes(int i, byte[] b, int offset) {
		b[offset+0] = (byte)((i >> 24) & 0xFF);
		b[offset+1] = (byte)((i >> 16) & 0xFF);
		b[offset+2] = (byte)((i >>  8) & 0xFF);
		b[offset+3] = (byte)((i >>  0) & 0xFF);
	}
	
	static byte[] intToBytes(int i) {
		byte[] bytes = new byte[4];
		intToBytes(i, bytes, 0);
		return bytes;
	}
	
	static int bytesToInt(byte[] b, int offset) {
		return
			((b[offset+0] << 24) & 0xFF000000) |
			((b[offset+1] << 16) & 0x00FF0000) |
			((b[offset+2] <<  8) & 0x0000FF00) |
			((b[offset+3] <<  0) & 0x000000FF);
	}
	
	public static int strToInt(String c) {
		try {
			return bytesToInt(c.getBytes("UTF-8"), 0);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String intToStr(int i) {
		try {
			return new String(intToBytes(i), "UTF-8");
		} catch( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
	}
	
	protected int readInt() throws IOException {
		return bytesToInt(readBytes(4, "readInt"), 0);
	}
	
	protected void writeInt(int value) throws IOException {
		byte[] b = new byte[4];
		intToBytes(value, b, 0);
		raf.write(b);
	}
	
	protected int getIntAt(int offset) throws IOException {
		raf.seek(offset);
		return readInt();
	}
	
	protected void setIntAt(int offset, int value) throws IOException {
		raf.seek(offset);
		writeInt(value);
	}
	
	protected byte[] readBytes(int count, String context) throws IOException {
		byte[] data = new byte[count];
		raf.readFully(data);
		return data;
	}
	
	public byte[] getBytes(int offset, int count) throws IOException {
		raf.seek(offset);
		return readBytes(count, "getBytes");
	}
	
	//// Edit the index ////
	
	public int getRawIndexItem(int itemNum) throws IOException {
		return getIntAt(INDEX_ITEMS_OFFSET + itemNum*4);
	}
	
	public int getIndexItemOffset(int itemNum) {
		return INDEX_ITEMS_OFFSET + (RESERVED_INDEX_ITEMS+itemNum)*4;
	}
	
	public int getIndexItem(int itemNum) throws IOException {
		if( itemNum < 0 || itemNum >= indexSize ) throw new IOException("Index " + itemNum + " out of range: 0..."+indexSize);
		return getRawIndexItem(itemNum + RESERVED_INDEX_ITEMS);
	}
	
	public int getCheckedIndexItem(int itemNum) throws IOException {
		int offset = getIndexItem(itemNum);
		return checkIndexPointer(offset, itemNum);
	}

	public void setRawIndexItem(int itemNum, int value) throws IOException {
		setIntAt(INDEX_ITEMS_OFFSET + itemNum*4, value);
	}

	public void setIndexItem(int itemNum, int value) throws IOException {
		if( itemNum < 0 || itemNum >= indexSize ) throw new IOException("Index " + itemNum + " out of range: 0..."+indexSize);
		setRawIndexItem(itemNum + RESERVED_INDEX_ITEMS, value);
	}
	
	protected void setLastChunkOffset(int offset) throws IOException {
		setRawIndexItem(ENDF_LIST_INDEX, offset);
	}
	
	public int getIndexRawItemCount( Chunk indexChunk ) throws IOException {
		return getIntAt( indexChunk.offset + CHUNK_HEADER_LENGTH );
	}
	public int[] getIndexItems( Chunk indexChunk ) throws IOException {
		int itemCount = getIndexRawItemCount( indexChunk );
		int[] items = new int[itemCount];
		for( int i=0; i<itemCount; ++i ) {
			items[i] = readInt();
		}
		return items;
	}

	//// Check / correct pointers ////
	
	protected boolean isPointerOk( int pointer ) throws IOException {
		return pointer >= HEADER_LENGTH && pointer < raf.length() - CHUNK_HEADER_LENGTH;
	}
	
	protected int checkIndexPointer( int pointer, int index ) throws IOException {
		if( pointer == 0 ) return 0;
		if( !isPointerOk(pointer) ) {
			throw new IOException("Bad index pointer at " + intToHex(getIndexItemOffset(index)) +
					" (index " + index + "): " + intToHex(pointer));
		}
		return pointer;
	}
	
	protected void checkPointer( int pointer, int pointerOffset ) throws IOException {
		if( !isPointerOk(pointer) ) {
			throw new IOException("Pointer at " + pointerOffset + " is bad, points to " + pointer );
		}
	}
	
	//// Load chunk ////
	
	public Chunk getChunk(int offset) throws IOException {
		if( offset == 0 ) return null;
		raf.seek(offset);
		byte[] header = readBytes(CHUNK_HEADER_LENGTH, "chunk header");
		Chunk chunk = new Chunk();
		chunk.offset = offset;
		int magic = bytesToInt(header, CHUNK_MAGIC_OFFSET); 
		if( magic != CHUNK_MAGIC ) {
			throw new IOException("Read malformed chunk at " + intToHex(offset) + " (bad magic: " + intToHex(magic));
		}
		chunk.prevOffset     = bytesToInt(header, CHUNK_PREV_OFFSET);
		chunk.nextOffset     = bytesToInt(header, CHUNK_NEXT_OFFSET);
		chunk.listPrevOffset = bytesToInt(header, CHUNK_LIST_PREV_OFFSET);
		chunk.listNextOffset = bytesToInt(header, CHUNK_LIST_NEXT_OFFSET);
		chunk.type           = bytesToInt(header, CHUNK_TYPE_OFFSET);
		return chunk;
	}
	
	public byte[] getChunkData(Chunk c) throws IOException {
		int start = c.offset + CHUNK_HEADER_LENGTH;
		int length = (c.nextOffset == 0) ? 0 : c.nextOffset - start;
		return getBytes(start,length);
	}

	//// Write a brand new chunk ////
	
	protected void writeChunk(Chunk chunk, byte[] data) throws IOException {
		raf.seek(chunk.offset);
		byte[] chunkHeader = new byte[CHUNK_HEADER_LENGTH];
		intToBytes(CHUNK_MAGIC,          chunkHeader, CHUNK_MAGIC_OFFSET);
		intToBytes(chunk.prevOffset,     chunkHeader, CHUNK_PREV_OFFSET);
		intToBytes(chunk.nextOffset,     chunkHeader, CHUNK_NEXT_OFFSET);
		intToBytes(chunk.listPrevOffset, chunkHeader, CHUNK_LIST_PREV_OFFSET);
		intToBytes(chunk.listNextOffset, chunkHeader, CHUNK_LIST_NEXT_OFFSET);
		intToBytes(chunk.type,           chunkHeader, CHUNK_TYPE_OFFSET);
		raf.write(chunkHeader);
		if( data != null ) raf.write(data);
	}
	
	protected Chunk writeEofChunk(int prevOffset) throws IOException {
		Chunk eofChunk = new Chunk();
		eofChunk.offset = (int)raf.getFilePointer();
		eofChunk.prevOffset = prevOffset;
		eofChunk.type = CHUNK_TYPE_ENDF;
		writeChunk(eofChunk, null);
		if( prevOffset != 0 ) setChunkNext(prevOffset, eofChunk.offset);
		return eofChunk;
	}
	
	public Chunk addChunkAtEnd(int listPrev, int listNext, int type, byte[] data) throws IOException {
		Chunk lastChunk = getLastChunk();
		Chunk newChunk = new Chunk();
		newChunk.offset = lastChunk.offset;
		newChunk.prevOffset = lastChunk.prevOffset;
		newChunk.listPrevOffset = listPrev;
		newChunk.listNextOffset = listNext;
		newChunk.type = type;
		writeChunk(newChunk, data);
		Chunk eofChunk = writeEofChunk(newChunk.offset);
		setLastChunkOffset(newChunk.nextOffset = eofChunk.offset);
		return newChunk;
	}

	//// In-place chunk editing ////
	
	protected void setChunkPrev(int chunkOffset, int value) throws IOException {
		setIntAt(chunkOffset+CHUNK_PREV_OFFSET, value);
	}
	protected void setChunkNext(int chunkOffset, int value) throws IOException {
		setIntAt(chunkOffset+CHUNK_NEXT_OFFSET, value);
	}
	protected void setChunkListPrev(int chunkOffset, int value) throws IOException {
		setIntAt(chunkOffset+CHUNK_LIST_PREV_OFFSET, value);
	}
	protected void setChunkListNext(int chunkOffset, int value) throws IOException {
		setIntAt(chunkOffset+CHUNK_LIST_NEXT_OFFSET, value);
	}
	protected void setChunkType(int chunkOffset, int value) throws IOException {
		setIntAt(chunkOffset+CHUNK_TYPE_OFFSET, value);
	}

	protected void setChunkPrev(Chunk chunk, int value) throws IOException {
		setIntAt(chunk.offset+CHUNK_PREV_OFFSET, value);
		chunk.prevOffset = value;
	}
	protected void setChunkNext(Chunk chunk, int value) throws IOException {
		setIntAt(chunk.offset+CHUNK_NEXT_OFFSET, value);
		chunk.nextOffset = value;
	}
	protected void setChunkListPrev(Chunk chunk, int value) throws IOException {
		setIntAt(chunk.offset+CHUNK_LIST_PREV_OFFSET, value);
		chunk.listPrevOffset = value;
	}
	protected void setChunkListNext(Chunk chunk, int value) throws IOException {
		setIntAt(chunk.offset+CHUNK_LIST_NEXT_OFFSET, value);
		chunk.listNextOffset = value;
	}
	protected void setChunkType(Chunk chunk, int value) throws IOException {
		setIntAt(chunk.offset+CHUNK_TYPE_OFFSET, value);
		chunk.type = value;
	}
	
	//// Find certain chunks ////
	
	public Chunk getFirstChunk() throws IOException {
		if( raf.length() <= HEADER_LENGTH ) return null;
		return getChunk(HEADER_LENGTH);
	}
	
	public Chunk getLastChunk() throws IOException {
		return getChunk(getRawIndexItem(ENDF_LIST_INDEX));
	}
	
	public Chunk getRecycleList() throws IOException {
		return getChunk(getRawIndexItem(RECL_LIST_INDEX));
	}
	
	public Chunk addChunk(int listPrev, int listNext, int type, byte[] data) throws IOException {
		Chunk recycleList = getRecycleList();
		if( recycleList != null ) {
			int next = recycleList.listNextOffset;
			while( next != 0 && next != recycleList.offset ) {
				Chunk c = getChunk(next);
				next = c.nextOffset;
				if( next - c.offset - CHUNK_HEADER_LENGTH >= data.length ) {
					// We can re-use this chunk!
					removeChunkFromList(c);
					setChunkType(c, type);
					raf.write(data);
					return c;
				}
			}
		}
		return addChunkAtEnd(listPrev, listNext, type, data);
	}
	
	public void recycleChunk( Chunk c ) throws IOException {
		removeChunkFromList(c);
		setChunkType(c, CHUNK_TYPE_RECL);
		addChunkToRawIndexList(1, c);
	}
	
	//// List manipulation ////
	
	public Chunk createList() throws IOException {
		Chunk newChunk = addChunk( 0, 0, CHUNK_TYPE_LIST, null );
		setChunkListNext(newChunk, newChunk.offset);
		setChunkListPrev(newChunk, newChunk.offset);
		return newChunk;
	}
	
	public void addChunkToList( int prev, int next, Chunk c ) throws IOException {
		setChunkListPrev( c, prev );
		setChunkListNext( prev, c.offset );
		setChunkListNext( c, next );
		setChunkListPrev( next, c.offset );
	}
	
	public void removeChunkFromList( Chunk c ) throws IOException {
		if( c.listPrevOffset != 0 ) setChunkListNext(c.listPrevOffset, c.listNextOffset);
		if( c.listNextOffset != 0 ) setChunkListNext(c.listNextOffset, c.listPrevOffset);
		c.listPrevOffset = 0;
		c.listNextOffset = 0;
	}
	
	public void addChunkToRawIndexList( int index, Chunk chunk ) throws IOException {
		int listOffset = getRawIndexItem(index);
		Chunk listChunk;
		if( listOffset == 0 ) {
			listChunk = createList();
			setRawIndexItem(index, listChunk.offset);
		} else {
			listChunk = getChunk(listOffset);
		}
		addChunkToList(listChunk.offset, listChunk.listNextOffset, chunk);
	}
	
	public Chunk addChunkToIndexList( int index, int type, byte[] data ) throws IOException {
		Chunk c = addChunk(0, 0, type, data);
		addChunkToRawIndexList( index+RESERVED_INDEX_ITEMS, c );
		return c;
	}
	
	//// Get/set pairs ////
	
	public Chunk getPairChunk( int index, byte[] identifier ) throws IOException {
		int listOffset = getCheckedIndexItem(index);
		if( listOffset == 0 ) return null;
		int itemOffset = listOffset;
		while( itemOffset != 0 ) {
			Chunk item = getChunk(itemOffset);
			if( item.type == CHUNK_TYPE_PAIR ) {
				byte[] key = getPairKey( itemOffset );
				if( Arrays.equals(identifier, key) ) return item; 
			}
			itemOffset = item.listNextOffset;
			if( itemOffset == listOffset ) return null;
		}
		return null;
	}
	
	protected byte[] getPairPart( int pairOffset, int partIndex ) throws IOException {
		int offset = pairOffset + CHUNK_HEADER_LENGTH;
		int partLength = getIntAt(offset);
		if( partLength == 0 ) return new byte[0];
		int pi=0;
		while( pi<partIndex ) {
			offset += 4;  offset += partLength;
			++pi;
			partLength = getIntAt(offset);
			if( partLength == 0 ) return null;
		}
		return readBytes(partLength, "getPairPart");
	}
	public byte[] getPairKey( int pairOffset ) throws IOException {
		return getPairPart(pairOffset, 0);
	}
	public byte[] getPairValue( int pairOffset ) throws IOException {
		return getPairPart(pairOffset, 1);
	}

	protected static byte[] encodePair(byte[] identifier, byte[] value) {
		byte[] data = new byte[identifier.length + value.length + 12];
		intToBytes(identifier.length, data, 0);
		for( int i=0, j=4; i<identifier.length; ++i, ++j ) {
			data[j] = identifier[i];
		}
		intToBytes(value.length, data, 4 + identifier.length);
		for( int i=0, j=8+identifier.length; i<value.length; ++i, ++j ) {
			data[j] = value[i];
		}
		intToBytes(0, data, 8 + identifier.length + value.length);
		return data;
	}
	
	public FileLock getReadLock() throws IOException {
		return raf.getChannel().lock(0l, Long.MAX_VALUE, true);
	}
	
	public FileLock getWriteLock() throws IOException {
		return raf.getChannel().lock(0l, Long.MAX_VALUE, false);
	}
	
	//// Mid-level functions to get/set pairs with file locking ////

	/**
	 * Find the value associated with the given key.
	 * If autoLockingEnabled is true, will aquire a shared lock on the underlying file during the operation. 
	 */
	public synchronized byte[] get( int index, byte[] identifier ) throws IOException {
		FileLock lock = autoLockingEnabled ? getReadLock() : null;
		try {
			Chunk pc = getPairChunk( index, identifier );
			if( pc == null ) return null;
			return getPairValue( pc.offset );
		} finally {
			if( lock != null ) lock.release();
		}
	}
	
	/**
	 * Associate the given key with the given value.
	 * If autoLockingEnabled is true, will aquire an exclusive lock on the underlying file during the operation. 
	 */
	public synchronized void put( int index, byte[] identifier, byte[] value ) throws IOException {
		FileLock lock = autoLockingEnabled ? getWriteLock() : null;
		try {
			Chunk pc = getPairChunk( index, identifier );
			if( pc != null ) {
				if( Arrays.equals(getPairValue(pc.offset), value) ) return;
				if( pc.nextOffset != 0 && pc.nextOffset - pc.offset - CHUNK_HEADER_LENGTH - identifier.length - value.length - 12 >= 0 ) {
					raf.seek(pc.offset + CHUNK_HEADER_LENGTH + identifier.length + 4);
					writeInt(value.length);
					raf.write(value);
					writeInt(0);
					return;
				}
				recycleChunk(pc);
			}
			addChunkToIndexList(index, CHUNK_TYPE_PAIR, encodePair(identifier, value));
		} finally {
			if( lock != null ) {
				raf.getChannel().force(true);
				lock.release();
			}
		}
	}
	
	/**
	 * A very simple (and not secure) hash function found at
	 * http://www.partow.net/programming/hashfunctions/
	 */
	public static int hash( byte[] identifier ) {
		int b    = 378551;
		int a    = 63689;
		int hash = 0;

		for( int i = 0; i < identifier.length; ++i ) {
			hash = hash * a + identifier[i];
			a *= b;
		}
		
		return hash & 0x7FFFFFFF;
	}
	
	public final int indexIndex( byte[] k ) {
		return hash(k)%indexSize;
	}
	
	public byte[] get( byte[] k ) throws IOException {
		return get( indexIndex(k), k );
	}
	
	public void put( byte[] k, byte[] v ) throws IOException {
		put( indexIndex(k), k, v );
	}
	
	public byte[] get( String k ) throws IOException {
		try {
			return get( k.getBytes("UTF-8") );
		} catch( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public void put( String k, byte[] v ) throws IOException {
		try {
			put( k.getBytes("UTF-8"), v );
		} catch( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[] args) {
		try {
			File f = new File(args[0]);
			SimpleListFile slf = new SimpleListFile(f, "r");
			Chunk c = slf.getFirstChunk();
			while( c != null ) {
				System.out.println(intToStr(c.type) + " at " + c.offset + ", lnext=" + c.listNextOffset + ", lprev=" + c.listPrevOffset + " (length=" + (c.nextOffset-c.offset) + ")");
				c = slf.getChunk(c.nextOffset);
			}
			slf.close();
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
}
