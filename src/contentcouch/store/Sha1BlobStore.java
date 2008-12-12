package contentcouch.store;

import org.bitpedia.util.Base32;

import contentcouch.data.Blob;
import contentcouch.digest.DigestUtil;

public class Sha1BlobStore implements BlobSource, BlobSink {
	protected BlobSource blobSource;
	protected BlobStore blobStore;
	
	public Sha1BlobStore( BlobSource blobSource, BlobStore blobStore ) {
		this.blobSource = blobSource;
		this.blobStore  = blobStore;
	}
	
	public Sha1BlobStore( FileBlobStore fbs ) {
		this( fbs, fbs );
	}

	public Sha1BlobStore( String filenamePrefix ) {
		this(new FileBlobStore(filenamePrefix));
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
		if( blobStore == null ) {
			throw new RuntimeException("Can't store blob because I lack a blob store");
		}
		byte[] sha1 = DigestUtil.sha1DigestBlob(blob);
		String urn = "urn:sha1:" + Base32.encode(sha1);
		String filename = getFilenameForSha1( sha1 );
		blobStore.put(filename, blob);
		return urn;
	}

	public Blob get( String urn ) {
		if( blobSource == null ) return null;
		if( urn.startsWith("urn:sha1:") ) {
			byte[] sha1 = Base32.decode(urn.substring(9));
			String filename = getFilenameForSha1( sha1 );
			return blobSource.get(filename);
		}
		return null;
	}
}
