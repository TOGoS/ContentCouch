// -*- tab-width:4 -*-
package contentcouch.store;

import java.io.File;

import org.bitpedia.util.Base32;

import contentcouch.data.Blob;
import contentcouch.data.FileBlob;
import contentcouch.digest.DigestUtil;
import contentcouch.hashcache.FileHashCache;

public class Sha1BlobStore implements BlobStore, FileGetter, FileForBlobGetter, UrnForBlobGetter {
	protected BlobGetter blobGetter;
	protected BlobPutter blobPutter;
	public FileHashCache fileHashCache;
	
	public Sha1BlobStore( BlobGetter blobSource, BlobPutter blobStore ) {
		this.blobGetter = blobSource;
		this.blobPutter = blobStore;
	}
	
	public Sha1BlobStore( BlobMap fbs ) {
		this( fbs, fbs );
	}

	public Sha1BlobStore( String filenamePrefix ) {
		this(new FileBlobMap(filenamePrefix));
	}
	
	protected String getFilenameForSha1( byte[] sha1 ) {
		/*
		char[] hex = DigestUtil.bytesToLowerHex(sha1);
		return new String(hex,0,2) + "/" + new String(hex);
		*/
		String base32 = Base32.encode(sha1);
		return new String( base32.substring(0,2) + "/" + base32);
	}

	protected String getUrnForSha1( byte[] sha1 ) {
		return "urn:sha1:" + Base32.encode(sha1);
	}
	
	protected byte[] getSha1( Blob blob ) {
		if( blob instanceof FileBlob && fileHashCache != null ) {
			return fileHashCache.getSha1((FileBlob)blob);
		} else {
			return DigestUtil.sha1DigestBlob(blob);
		}
	}

	public String getUrnForBlob( Blob blob ) {
		return getUrnForSha1( getSha1(blob) );
	}
	
	public String push( Blob blob ) {
		if( blobPutter == null ) {
			throw new RuntimeException("Can't store blob because I lack a blob store");
		}
		byte[] sha1 = getSha1(blob);
		String urn = getUrnForSha1( sha1 );
		String filename = getFilenameForSha1( sha1 );
		blobPutter.put(filename, blob);
		if( blobPutter instanceof FileGetter ) {
			File f = ((FileGetter)blobPutter).getFile(filename);
			if( f != null ) {
				f.setReadOnly();
			}
		}
		return urn;
	}

	public Blob get( String urn ) {
		if( blobGetter == null ) return null;
		if( urn.startsWith("urn:sha1:") ) {
			byte[] sha1 = Base32.decode(urn.substring(9));
			String filename = getFilenameForSha1( sha1 );
			return blobGetter.get(filename);
		}
		return null;
	}
	
	public File getFile( String urn ) {
		if( urn.startsWith("urn:sha1:") && (blobGetter instanceof FileGetter) ) {
			byte[] sha1 = Base32.decode(urn.substring(9));
			String filename = getFilenameForSha1( sha1 );
			return ((FileGetter)blobGetter).getFile(filename);
		}
		return null;
	}

	public File getFileForBlob( Blob blob ) {
		if( blobGetter instanceof FileGetter ) {
			byte[] sha1 = getSha1(blob);
			String filename = getFilenameForSha1( sha1 );
			return ((FileGetter)blobGetter).getFile(filename);
		}
		return null;		
	}
}
