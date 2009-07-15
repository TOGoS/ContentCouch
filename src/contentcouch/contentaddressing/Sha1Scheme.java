package contentcouch.contentaddressing;

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
		return CcouchNamespace.SHA1BASE32;
	}
	
	public static String SHA1URNPREFIX = "urn:sha1:";
	public static int SHA1BASE32LENGTH = 32;
	
	/** Return true if the given URN is in the domain of this addressing scheme */ 
	public boolean wouldHandleUrn( String urn ) {
		return urn.startsWith(SHA1URNPREFIX) || urn.startsWith(BitprintScheme.BITPRINTURNPREFIX);
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
		return SHA1URNPREFIX + Base32.encode(hash);
	}
	public byte[] urnToHash( String urn ) {
		String b32 = urn;
		if( urn.startsWith(SHA1URNPREFIX) ) {
			b32 = urn.substring(SHA1URNPREFIX.length(),SHA1URNPREFIX.length()+SHA1BASE32LENGTH);
		} else if( urn.startsWith(BitprintScheme.BITPRINTURNPREFIX) ) {
			b32 = urn.substring(BitprintScheme.BITPRINTURNPREFIX.length(),BitprintScheme.BITPRINTURNPREFIX.length()+SHA1BASE32LENGTH);
		} else {
			throw new BadlyFormedUrnException(urn);
		}
		if( b32.length() < SHA1BASE32LENGTH ) throw new BadlyFormedUrnException(urn);
		return Base32.decode(b32);
	}
	
	// Convert to/from filename
	public String hashToFilename( byte[] hash ) {
		return Base32.encode(hash);
	}
	public byte[] filenameToHash( String filename ) {
		if( filename.length() < SHA1BASE32LENGTH ) throw new BadlyFormedFilenameException(filename);
		return Base32.decode(filename);
	}
}
