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
 *   [4-byte number of entries]
 *   [4-byte hash code][offset to item]
 *   ...
 * 
 * normal hash codes should all be positive integers.
 * negative hash codes are special.
 * 
 *   -1 = unused (points nowhere)
 *   -2 = last (should be 'ENDF') chunk in file
 *   -3 = recycle list
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
	
	public static class Index {
		public Chunk firstChunkWithFreeSpots; 
		public HashMap entries;
		public Index() { this.entries = new HashMap(); }
	}

	public static class IndexEntry {
		int offset;
		int indexCode;
		int targetOffset;
	}

	public static int HEADER_LENGTH = 128;
	public static int CHUNK_HEADER_LENGTH = 4*5;
	
	RandomAccessFile raf;
	protected boolean writeMode;
	
	public SimpleListFile(File file, String mode) throws IOException {
		this.raf = new RandomAccessFile(file, mode);
		writeMode = mode.contains("w");
	}
	
	int bytesToInt(byte[] b, int offset) {
		return
			((b[offset+0] << 24) & 0xFF000000) |
			((b[offset+1] << 16) & 0x00FF0000) |
			((b[offset+2] <<  8) & 0x0000FF00) |
			((b[offset+3] <<  0) & 0x000000FF);
	}
	
	int strToInt(String c) {
		try {
			return bytesToInt(c.getBytes("UTF-8"), 0);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Chunk getChunk(int offset) throws IOException {
		raf.seek(offset);
		byte[] header = readBytes(CHUNK_HEADER_LENGTH, "chunk header");
		Chunk chunk = new Chunk();
		chunk.offset = offset;
		chunk.prevOffset = bytesToInt(header, offset+0);
		chunk.nextOffset = bytesToInt(header, offset+4);
		chunk.listPrevOffset = bytesToInt(header, offset+8);
		chunk.listNextOffset = bytesToInt(header, offset+12);
		chunk.type = bytesToInt(header, offset+16);
		return null;
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
	
	protected byte[] readBytes(int count, String context) throws IOException {
		byte[] data = new byte[count];
		int bytesRead = raf.read(data);
		if( bytesRead < count ) throw new IOException("Only read " + bytesRead + " of expected " + count + " bytes while reading " + context + " at " + (raf.getFilePointer() - bytesRead));
		return data;
	}
	
	protected void loadIndexChunkIntoIndex(Chunk c, Index i) throws IOException {
		int numEntries = getIntAt(c.offset+CHUNK_HEADER_LENGTH);
		if( numEntries < 0 ) throw new IOException("Number of index entries is < 0!");
		if( numEntries > 65536*256 ) throw new IOException("Number of index entries is too large: " + numEntries);
		byte[] indexData = readBytes(numEntries*8, "index data");
		for( int e=0; e<numEntries; ++e ) {
			int indexCode = bytesToInt(indexData, e*8);
			if( indexCode == -1 ) {
				if( i.firstChunkWithFreeSpots == null ) i.firstChunkWithFreeSpots = c;
			} else {
				IndexEntry ie = new IndexEntry();
				ie.offset = c.offset+CHUNK_HEADER_LENGTH+4+(e*8);
				ie.indexCode = indexCode;
				ie.targetOffset = c.offset+24;
				i.entries.put(Integer.valueOf(ie.indexCode), ie);
			}
		}
	}
	
	public Index getIndex() throws IOException {
		if( this.raf.length() <= HEADER_LENGTH ) return null;
		Chunk indexChunk = getChunk(HEADER_LENGTH);
		Index index = new Index();
		loadIndexChunkIntoIndex(indexChunk, index);
		while( indexChunk.listNextOffset != 0 ) {
			indexChunk = getChunk(indexChunk.listNextOffset);
			loadIndexChunkIntoIndex(indexChunk, index);
		}
		if( index.firstChunkWithFreeSpots == null ) {
			index.firstChunkWithFreeSpots = indexChunk;
		}
		return index;
	}

	public void writeChunk(int offset, int listPrev, int listNext, int type, byte[] data) {
		
	}
	
	public void addChunk(int listPrev, int listNext, int type, byte[] data) {
		
	}
	
	protected void createIndexData(int numEntries) {
		
	}
	
	public void init(int nIndexEntries, int fileSize) throws IOException {
		if( this.raf.length() == 0 ) {
			if( writeMode ) {
				this.raf.setLength(HEADER_LENGTH);
			} else {
				return;
			}
		}
		if( this.raf.length() == HEADER_LENGTH ) {
			if( writeMode ) {
				//addChunk( )
			} else {
				return;
			}
		}
	}
	
	public void close() throws IOException {
		this.raf.close();
	}
}
