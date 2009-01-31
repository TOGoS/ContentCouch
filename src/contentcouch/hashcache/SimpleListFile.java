package contentcouch.hashcache;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/** Format of this kind of file:
 * 
 * [header]
 * [index chunk]
 * [data and empty chunks]
 * 
 * 
 * header:     [128 bytes of padding]
 * 
 * 
 * chunk:
 *   [4-byte offset of physical prev chunk]
 *   [4-byte offset of physical next chunk]
 *   [4-byte offset of list prev chunk]
 *   [4-byte offset of list next chunk]
 *   [chunk type]
 *   [chunk content]
 *   
 * chunk types:
 *   'RECL' - recycle (it is blank)
 *   'INDX' - index
 *   'LIST' - placeholder for first/last item in a list
 *   'PAIR' - dictionary item
 *   'ENDF' - end of file
 * 
 * recycle item chunk content:
 * 
 * dictionary item chunk content:
 *   [4-byte length of name]
 *   [4-byte length of data]
 *   [name]
 *   [data]
 *   
 * data chunk content is like:
 * 
 * index:
 *   [4-byte number of entries (incl. reserved ones)]
 *   [offset of last chunk in file]
 *   [offset to recycle list]
 *   [13 more reserved entries]
 *   [offset to items]
 *  
 * each entry: [4-byte length][n bytes]
 * last entry: [4-byte -1]
 */
public class SimpleListFile {
	public static class Chunk {
		int offset;
		int prevOffset;
		int nextOffset;
		int listPrevOffset;
		int listNextOffset;
		int type;
	}
	
	public static int HEADER_LENGTH = 128;
	public static int CHUNK_HEADER_LENGTH = 4*5;
	public static int CHUNK_PREV_OFFSET = 0;
	public static int CHUNK_NEXT_OFFSET = 4;
	public static int CHUNK_LIST_PREV_OFFSET = 8;
	public static int CHUNK_LIST_NEXT_OFFSET = 12;
	public static int CHUNK_TYPE_OFFSET = 16;
	
	public static int INDEX_CHUNK_OFFSET = HEADER_LENGTH;
	public static int INDEX_ITEMS_OFFSET = INDEX_CHUNK_OFFSET + CHUNK_HEADER_LENGTH + 4;
	public static int LAST_CHUNK_OFFSET_OFFSET = INDEX_ITEMS_OFFSET + 0;
	public static int EMPTY_LIST_OFFSET_OFFSET = INDEX_ITEMS_OFFSET + 4;
	public static int RESERVED_INDEX_ITEMS = 15;
	public static int USER_INDEX_ITEMS_OFFSET = INDEX_ITEMS_OFFSET + RESERVED_INDEX_ITEMS*4;
	
	public static int CHUNK_TYPE_RECL = strToInt("RECL");
	public static int CHUNK_TYPE_INDX = strToInt("INDX");
	public static int CHUNK_TYPE_LIST = strToInt("LIST");
	public static int CHUNK_TYPE_PAIR = strToInt("PAIR");
	public static int CHUNK_TYPE_ENDF = strToInt("ENDF");
	
	RandomAccessFile raf;
	protected boolean writeMode;
	
	public SimpleListFile(File file, String mode) throws IOException {
		this.raf = new RandomAccessFile(file, mode);
		writeMode = mode.contains("w");
	}
	
	public void init(int numIndexEntries, int fileSize) throws IOException {
		if( this.raf.length() <= HEADER_LENGTH ) {
			if( !writeMode ) throw new IOException("Can't initialize unless in write mode");
			this.raf.setLength(fileSize);
			Chunk indexChunk = new Chunk();
			indexChunk.offset = INDEX_CHUNK_OFFSET;
			indexChunk.type = CHUNK_TYPE_INDX;
			writeChunk(indexChunk, createIndexData(numIndexEntries+RESERVED_INDEX_ITEMS));
			Chunk eofChunk = writeEofChunk(indexChunk.offset);
			setLastChunkOffset(eofChunk.offset);
		}
	}
	
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
	
	static int strToInt(String c) {
		try {
			return bytesToInt(c.getBytes("UTF-8"), 0);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	static String intToStr(int i) {
		try {
			return new String(intToBytes(i), "UTF-8");
		} catch( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
	}
	
	protected int readInt() throws IOException {
		byte[] buf = new byte[4];
		int bytesRead = raf.read(buf);
		if( bytesRead < 4 ) throw new IOException("Couldn't read integer at " +
				(raf.getFilePointer() - bytesRead) + "; only " + bytesRead + " of 4 bytes read");
		return bytesToInt(buf, 0);
	}
	
	protected int getIntAt(int offset) throws IOException {
		raf.seek(offset);
		return readInt();
	}
	
	protected void setIntAt(int offset, int value) throws IOException {
		byte[] b = new byte[4];
		intToBytes(value, b, 0);
		raf.seek(offset);
		raf.write(b);
	}
	
	protected byte[] readBytes(int count, String context) throws IOException {
		byte[] data = new byte[count];
		int bytesRead = raf.read(data);
		if( bytesRead < count ) throw new IOException("Only read " + bytesRead + " of expected " + count + " bytes while reading " + context + " at " + (raf.getFilePointer() - bytesRead));
		return data;
	}
	
	public int getIndexItem(int itemNum) throws IOException {
		itemNum += RESERVED_INDEX_ITEMS;
		return getIntAt(INDEX_ITEMS_OFFSET + itemNum*4);
	}
	
	public void setIndexItem(int itemNum, int value) throws IOException {
		itemNum += RESERVED_INDEX_ITEMS;
		setIntAt(INDEX_ITEMS_OFFSET + itemNum*4, value);
	}
	
	public Chunk getChunk(int offset) throws IOException {
		if( offset == 0 ) return null;
		raf.seek(offset);
		byte[] header = readBytes(CHUNK_HEADER_LENGTH, "chunk header");
		Chunk chunk = new Chunk();
		chunk.offset = offset;
		chunk.prevOffset     = bytesToInt(header, CHUNK_PREV_OFFSET);
		chunk.nextOffset     = bytesToInt(header, CHUNK_NEXT_OFFSET);
		chunk.listPrevOffset = bytesToInt(header, CHUNK_LIST_PREV_OFFSET);
		chunk.listNextOffset = bytesToInt(header, CHUNK_LIST_NEXT_OFFSET);
		chunk.type           = bytesToInt(header, CHUNK_TYPE_OFFSET);
		return chunk;
	}

	public void writeChunk(Chunk chunk, byte[] data) throws IOException {
		raf.seek(chunk.offset);
		byte[] chunkHeader = new byte[CHUNK_HEADER_LENGTH];
		intToBytes(chunk.prevOffset,     chunkHeader, CHUNK_PREV_OFFSET);
		intToBytes(chunk.nextOffset,     chunkHeader, CHUNK_NEXT_OFFSET);
		intToBytes(chunk.listPrevOffset, chunkHeader, CHUNK_LIST_PREV_OFFSET);
		intToBytes(chunk.listNextOffset, chunkHeader, CHUNK_LIST_NEXT_OFFSET);
		intToBytes(chunk.type,           chunkHeader, CHUNK_TYPE_OFFSET);
		raf.write(chunkHeader);
		if( data != null ) raf.write(data);
	}
	
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
	
	public Chunk getFirstChunk() throws IOException {
		if( raf.length() <= HEADER_LENGTH ) return null;
		return getChunk(HEADER_LENGTH);
	}
	
	public Chunk getLastChunk() throws IOException {
		return getChunk(getIntAt(LAST_CHUNK_OFFSET_OFFSET));
	}
	
	protected void setLastChunkOffset(int offset) throws IOException {
		setIntAt(LAST_CHUNK_OFFSET_OFFSET, offset);
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
	
	protected byte[] createIndexData(int numEntries) {
		byte[] dat = new byte[numEntries+4];
		intToBytes(numEntries, dat, 0);
		return dat;
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
	
	public Chunk addChunk(int listPrev, int listNext, int type, byte[] data) throws IOException {
		// Eventually this should try to use the recycle list.
		return addChunkAtEnd(listPrev, listNext, type, data);
	}
	
	public Chunk addChunkToList( int prev, int next, int type, byte[] data ) throws IOException {
		Chunk newChunk = addChunk( prev, next, type, data );
		if( prev != 0 ) setChunkListNext(prev, newChunk.offset);
		if( next != 0 ) setChunkListPrev(next, newChunk.offset);
		return newChunk;
	}
	
	public Chunk addChunkToIndexList( int index, int type, byte[] data ) throws IOException {
		int listOffset = getIndexItem(index);
		Chunk listChunk;
		if( listOffset == 0 ) {
			listChunk = addChunk(0, 0, CHUNK_TYPE_LIST, null);
			setChunkPrev(listChunk, listChunk.offset);
			setChunkNext(listChunk, listChunk.offset);
			setIndexItem(index, listChunk.offset);
		} else {
			listChunk = getChunk(listOffset);
		}
		return addChunk(listChunk.offset, listChunk.listNextOffset, type, data);
	}
	
	public byte[] get( int index, byte[] identifier ) {
		return null;
	}
	
	public void put( int index, byte[] identifier, byte[] value ) {
		
	}
	
	public void close() throws IOException {
		this.raf.close();
	}
	
	public static void main(String[] args) {
		try {
			File f = new File("junk/test.slf");
			SimpleListFile slf = new SimpleListFile(f, "rw");
			slf.init(65536, 1024*1024);
			slf.addChunkAtEnd(0, 0, CHUNK_TYPE_RECL, new byte[8]);
			slf.close();
			
			slf = new SimpleListFile(f, "rw");
			Chunk c = slf.getFirstChunk();
			while( c != null ) {
				System.out.println(intToStr(c.type) + " at " + c.offset + ", next=" + c.nextOffset + ", prev=" + c.prevOffset);
				c = slf.getChunk(c.nextOffset);
			}
			slf.close();
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
}
