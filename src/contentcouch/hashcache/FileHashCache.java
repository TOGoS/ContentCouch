package contentcouch.hashcache;

import java.io.File;
import java.io.IOException;

import org.bitpedia.util.Base32;

import contentcouch.file.FileBlob;
import contentcouch.file.FileUtil;
import contentcouch.repository.ContentAddressingScheme;
import contentcouch.repository.Sha1Scheme;



public class FileHashCache {
	public static class Entry {
		public long size;
		public long mtime;
		public byte[] hash;
		
		protected static void longToBytes(long l, byte[] bytes, int offset) {
			bytes[offset+0] = (byte)((l >> 56) & 0xFF);
			bytes[offset+1] = (byte)((l >> 48) & 0xFF);
			bytes[offset+2] = (byte)((l >> 40) & 0xFF);
			bytes[offset+3] = (byte)((l >> 32) & 0xFF);
			bytes[offset+4] = (byte)((l >> 24) & 0xFF);
			bytes[offset+5] = (byte)((l >> 16) & 0xFF);
			bytes[offset+6] = (byte)((l >>  8) & 0xFF);
			bytes[offset+7] = (byte)((l >>  0) & 0xFF);
		}
		
		protected static long bytesToLong(byte[] bytes, int offset) {
			return
				(((long)bytes[offset+0] << 56) & 0xFF00000000000000l) |
				(((long)bytes[offset+1] << 48) & 0x00FF000000000000l) |
				(((long)bytes[offset+2] << 40) & 0x0000FF0000000000l) |
				(((long)bytes[offset+3] << 32) & 0x000000FF00000000l) |
				(((long)bytes[offset+4] << 24) & 0x00000000FF000000l) |
				(((long)bytes[offset+5] << 16) & 0x0000000000FF0000l) |
				(((long)bytes[offset+6] <<  8) & 0x000000000000FF00l) |
				(((long)bytes[offset+7] <<  0) & 0x00000000000000FFl);
		}
		
		public byte[] toBytes() {
			byte[] bytes = new byte[36];
			longToBytes(this.size, bytes, 0);
			longToBytes(this.mtime, bytes, 8);
			for(int i=0; i<hash.length; ++i) bytes[16+i] = hash[i];
			return bytes;
		}
		
		public static Entry fromBytes( byte[] bytes ) {
			if( bytes.length != 36 ) {
				throw new RuntimeException("Wrong length for Entry.fromBytes input: " + bytes.length + " (should be 36 - 8 size, 8 mtime, 20 sha-1)");
			}
			Entry e = new Entry();
			e.size = bytesToLong( bytes, 0 );
			e.mtime = bytesToLong( bytes, 8 );
			e.hash = new byte[bytes.length-16];
			for( int i=0; i<e.hash.length; ++i ) e.hash[i] = bytes[16+i];
			return e;
		}
	}
	
	protected File cacheFile;
	protected SimpleListFile slf;
	protected String mode;
	
	protected SimpleListFile getSlf() {
		if( slf == null ) {
			try {
				FileUtil.mkParentDirs(cacheFile);
				slf = new SimpleListFile(cacheFile, mode);
				slf.init(65536, 1024*1024);
			} catch( IOException e ) {
				if( mode.indexOf('w') != -1 ) {
					System.err.println("Couldn't open cache file in '" + mode + "' mode, trying again as 'r'");
					try { 
						slf = new SimpleListFile(cacheFile, "r");
					} catch( IOException ee ) {
						throw new RuntimeException( ee );
					}
				} else {
					throw new RuntimeException("Couldn't open cache file in '" + mode + "' mode", e);
				}
			}
		}
		return slf;
	}
	
	public FileHashCache(File cacheFile, String mode) {
		this.cacheFile = cacheFile;
		this.mode = mode;
	}
	
	public FileHashCache(File cacheFile) {
		this(cacheFile, "rw");
	}

	public Entry getCachedEntry( File file ) throws IOException {
		byte[] eb = getSlf().get(file.getCanonicalPath());
		if( eb == null ) return null;
		return Entry.fromBytes(eb);
	}
	
	public Entry getCachedValidEntry( File file ) throws IOException {
		Entry e = getCachedEntry( file );
		return (e != null && file.lastModified() == e.mtime && file.length() == e.size) ? e : null;
	}
	
	public byte[] getHash( FileBlob file, ContentAddressingScheme scheme ) {
		try {
			Entry e = getCachedValidEntry(file);
			if( e != null ) {
				return e.hash;
			}
			byte[] hash = scheme.getHash(file);
			if( getSlf().isWritable() ) {
				e = new Entry();
				e.mtime = file.lastModified();
				e.size = file.length();
				e.hash = hash;
				getSlf().put(file.getCanonicalPath(), e.toBytes());
			}
			return hash;
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[] args) {
		try {
			File slff = new File("C:/stuff/proj/ContentCouch/junk-repo/cache/file-info.slf");
			FileHashCache fhc = new FileHashCache(slff, "rw");
			//System.err.println(fhc.getSha1(new FileBlob(new File("F:/archives/apps/Reason/Reason-4.0-AiR/Reason 4.iso"))));
			System.err.println(Base32.encode(fhc.getHash(new FileBlob(new File("C:/stuff/proj/ContentCouch/.classpath")), new Sha1Scheme())));
		} catch( Exception e ) {
			throw new RuntimeException(e);
		}
	}
	
	public void close() throws IOException {
		if( slf != null ) {
			slf.close();
			slf = null;
		}
	}
}
