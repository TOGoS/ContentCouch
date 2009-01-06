package contentcouch.store;

import java.io.File;

import org.bitpedia.util.Base32;

import contentcouch.data.Blob;
import contentcouch.digest.DigestUtil;

public class Sha1BlobStore implements BlobStore, FileGetter {
	protected BlobGetter blobGetter;
	protected BlobPutter blobPutter;
	
	public Sha1BlobStore( BlobGetter blobSource, BlobPutter blobStore ) {
		this.blobGetter = blobSource;
		this.blobPutter  = blobStore;
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
	
	public String push( Blob blob ) {
		if( blobPutter == null ) {
			throw new RuntimeException("Can't store blob because I lack a blob store");
		}
		byte[] sha1 = DigestUtil.sha1DigestBlob(blob);
		String urn = "urn:sha1:" + Base32.encode(sha1);
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
}
