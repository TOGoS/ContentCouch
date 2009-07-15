package contentcouch.contentaddressing;

import org.bitpedia.util.Base32;

import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Blob;

public class BitprintScheme implements ContentAddressingScheme {
	public static final BitprintScheme instance = new BitprintScheme();
	public static final BitprintScheme getInstance() { return instance; }
	
	public String getSchemeDisplayName() {
		return "Bitprint";
	}
	public String getRdfKey() {
		return CcouchNamespace.BITPRINT;
	}
	
	public static String BITPRINTURNPREFIX = "urn:bitprint:";
	
	/** Return true if the given URN is in the domain of this addressing scheme */ 
	public boolean wouldHandleUrn( String urn ) {
		return urn.startsWith(BITPRINTURNPREFIX);
	}
	
	protected byte[] joinHashes( byte[] sha1Hash, byte[] tigerTreeHash ) {
		byte[] hash = new byte[44];
		System.arraycopy( sha1Hash, 0, hash, 0, 20);
		System.arraycopy( tigerTreeHash, 0, hash, 20, 24);
		return hash;
	}

	/** Return the canonical identifier of the given blob */
	public byte[] getHash( Blob blob ) {
		return joinHashes( Sha1Scheme.getInstance().getHash(blob), TigerTreeScheme.getInstance().getHash(blob) );
	}
	
	// Convert to/from RDF value
	public String hashToRdfValue( byte[] hash ) {
		return hashToFilename(hash);
	}
	public byte[] rdfValueToHash( String value ) {
		return filenameToHash(value);
	}

	// Convert to/from URN
	public String hashToUrn( byte[] hash ) {
		return BITPRINTURNPREFIX + hashToFilename(hash);
	}
	
	public byte[] urnToHash( String urn ) {
		if( urn.startsWith(BITPRINTURNPREFIX) ) {
			return filenameToHash(urn.substring(BITPRINTURNPREFIX.length()));
		} else {
			throw new BadlyFormedUrnException(urn);
		}
	}
	
	// Convert to/from filename
	public String hashToFilename( byte[] hash ) {
		byte[] sha1Hash = new byte[20];
		System.arraycopy( hash, 0, sha1Hash, 0, 20);
		byte[] tigerTreeHash = new byte[24];
		System.arraycopy( hash, 20, tigerTreeHash, 0, 24);
		return Base32.encode(sha1Hash) + "." + Base32.encode(tigerTreeHash);
	}
	public byte[] filenameToHash( String filename ) {
		String sha1Base32 = filename.substring(0, Sha1Scheme.SHA1BASE32LENGTH);
		String tigerTreeBase32 = filename.substring(Sha1Scheme.SHA1BASE32LENGTH + 1, Sha1Scheme.SHA1BASE32LENGTH + 1 + TigerTreeScheme.TIGERTREEBASE32LENGTH);
		return joinHashes( Base32.decode(sha1Base32), Base32.decode(tigerTreeBase32) );
	}
}
