package contentcouch.repository;

import org.bitpedia.util.Base32;

import contentcouch.digest.DigestUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Blob;

public class Sha1Scheme implements ContentAddressingScheme {
	public static final Sha1Scheme instance = new Sha1Scheme();
	public static final Sha1Scheme getInstance() { return instance; }
	
	
	public String getSchemeDisplayName() {
		return "Base32-encoded SHA-1";
	}
	public String getRdfKey() {
		return CcouchNamespace.CCOUCH_BASE32SHA1;
	}
	
	protected static String SHA1URNPREFIX = "urn:sha1:";
	protected static String BITPRINTURNPREFIX = "urn:bitprint:";
	
	/** Return true if the given URN is in the domain of this addressing scheme */ 
	public boolean wouldHandleUrn( String urn ) {
		return urn.startsWith(SHA1URNPREFIX) || urn.startsWith(BITPRINTURNPREFIX);
	}

	/** Return the canonical identifier of the given blob */
	public byte[] getHash( Blob blob ) {
		return DigestUtil.sha1DigestBlob(blob);
	}
	
	// Convert to/from RDF value
	public String hashToRdfValue( byte[] hash ) {
		return Base32.encode(hash);
	}
	public byte[] rdfValueToHash( String value ) {
		if( value.length() < 32 ) throw new BadlyFormedRdfValueException(value);
		return Base32.decode(value);
	}

	// Convert to/from URN
	public String hashToUrn( byte[] hash ) {
		return "urn:sha1:" + Base32.encode(hash);
	}
	public byte[] urnToHash( String urn ) {
		String b32 = urn;
		if( urn.startsWith(SHA1URNPREFIX) ) {
			b32 = urn.substring(SHA1URNPREFIX.length(),SHA1URNPREFIX.length()+32);
		} else if( urn.startsWith(BITPRINTURNPREFIX) ) {
			b32 = urn.substring(BITPRINTURNPREFIX.length(),BITPRINTURNPREFIX.length()+32);
		} else {
			throw new BadlyFormedUrnException(urn);
		}
		if( b32.length() < 32 ) throw new BadlyFormedUrnException(urn);
		return Base32.decode(b32);
	}
	
	// Convert to/from filename
	public String hashToFilename( byte[] hash ) {
		return Base32.encode(hash);
	}
	public byte[] filenameToHash( String filename ) {
		if( filename.length() < 32 ) throw new BadlyFormedFilenameException(filename);
		return Base32.decode(filename);
	}
}
