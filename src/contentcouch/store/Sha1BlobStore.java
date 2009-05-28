// -*- tab-width:4 -*-
package contentcouch.store;

import java.io.File;

import org.bitpedia.util.Base32;

import contentcouch.blob.BlobUtil;
import contentcouch.digest.DigestUtil;
import contentcouch.file.FileBlob;
import contentcouch.hashcache.FileHashCache;
import contentcouch.path.PathUtil;
import contentcouch.value.Blob;
import contentcouch.value.MetadataHaver;

public class Sha1BlobStore implements BlobStore, StoreFileGetter, Identifier {
	protected Getter blobGetter;
	protected Putter blobPutter;
	public FileHashCache fileHashCache;
	
	public Sha1BlobStore( Getter blobSource, Putter blobStore ) {
		this.blobGetter = blobSource;
		this.blobPutter = blobStore;
	}
	
	public Sha1BlobStore( PutterGetter fbs ) {
		this( fbs, fbs );
	}

	public Sha1BlobStore( String uriPrefix ) {
		// TODO: Remove this dumb ambiguous constructor
		if( PathUtil.isUri(uriPrefix) ) {
			this.blobGetter = new PrefixGetFilter(TheGetter.getGenericGetter(), uriPrefix);
			this.blobPutter = null;
		} else {
			PutterGetter bbb = new FileBlobMap(uriPrefix);
			this.blobGetter = bbb;
			this.blobPutter = bbb; 
		}
	}
	
	protected String getFilenameForSha1( byte[] sha1 ) {
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
	
	protected static final String SHA1URN = "sha-1-urn"; 

	protected void cacheUrn( Object blob, String urn ) {
		if( blob instanceof MetadataHaver ) {
			((MetadataHaver)blob).putMetadata(SHA1URN, urn);
		}		
	}
	
	public String identify( Object blob ) {
		String urn;
		if( blob instanceof MetadataHaver ) {
			urn = (String)((MetadataHaver)blob).getMetadata(SHA1URN);
			if( urn != null ) return urn;
		}
		if( blob instanceof Blob ) {
			urn = getUrnForSha1( getSha1((Blob)blob) );
			cacheUrn(blob, urn);
			return urn;
		} else {
			throw new RuntimeException("Can't identify " + blob.getClass().getName());
		}
	}
	
	public String identifyAt( String uri ) {
		if( uri.startsWith("urn:sha1:") ) {
			if( uri.length() < 41 ) throw new RuntimeException("Badly formed SHA-1 URN: " + uri);
			return uri;
		} else if( uri.startsWith("urn:bitprint:") ) {
			if( uri.length() < 13+32 ) throw new RuntimeException("Badly formed bitprint URN: " + uri);
			return "urn:sha1:" + uri.substring(13, 13+32);
		} else {
			return identify(TheGetter.get(uri));
		}
	}
	
	public String push( Object obj ) {
		if( blobPutter == null ) {
			throw new RuntimeException("Can't store blob because I lack a blob store");
		}
		Blob blob = BlobUtil.getBlob(obj);
		byte[] sha1 = getSha1(blob);
		String urn = getUrnForSha1( sha1 );
		cacheUrn(blob, urn);
		String filename = getFilenameForSha1( sha1 );
		blobPutter.put(filename, blob);
		if( blobPutter instanceof StoreFileGetter ) {
			File f = ((StoreFileGetter)blobPutter).getStoreFile(filename);
			if( f != null ) {
				f.setReadOnly();
			}
		}
		return urn;
	}
	
	protected byte[] getSha1FromUrn( String urn ) {
		if( urn.startsWith("urn:sha1:") ) {
			if( urn.length() < 41 ) throw new RuntimeException("Badly formed SHA-1 URN: " + urn);
			return Base32.decode(urn.substring(9, 9+32));
		} else if( urn.startsWith("urn:bitprint:") ) {
			if( urn.length() < 13+32 ) throw new RuntimeException("Badly formed bitprint URN: " + urn);
			return Base32.decode(urn.substring(13, 13+32));
		} else {
			return null;
		}
	}

	public Object get( String urn ) {
		if( blobGetter == null ) return null;
		byte[] sha1 = getSha1FromUrn( urn );
		if( sha1 != null ) {
			String filename = getFilenameForSha1( sha1 );
			Object blob = blobGetter.get(filename);
			cacheUrn(blob, urn);
			return blob;
		}
		return null;
	}
	
	public File getStoreFile( String urn ) {
		if( urn.startsWith("urn:sha1:") && (blobGetter instanceof StoreFileGetter) ) {
			byte[] sha1 = Base32.decode(urn.substring(9));
			String filename = getFilenameForSha1( sha1 );
			return ((StoreFileGetter)blobGetter).getStoreFile(filename);
		}
		return null;
	}

	public File getStoreFile( Blob blob ) {
		if( blobGetter instanceof StoreFileGetter ) {
			byte[] sha1 = getSha1(blob);
			String filename = getFilenameForSha1( sha1 );
			return ((StoreFileGetter)blobGetter).getStoreFile(filename);
		}
		return null;		
	}
}
