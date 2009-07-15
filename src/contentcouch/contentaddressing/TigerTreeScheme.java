package contentcouch.contentaddressing;

import org.bitpedia.util.Base32;
import org.bitpedia.util.TigerTree;

import contentcouch.digest.DigestUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Blob;

public class TigerTreeScheme implements ContentAddressingScheme {
	public static final TigerTreeScheme instance = new TigerTreeScheme();
	public static final TigerTreeScheme getInstance() { return instance; }
	
	
	public String getSchemeDisplayName() {
		return "Base32-encoded Tiger-Tree";
	}
	public String getRdfKey() {
		return CcouchNamespace.BASE32TIGERTREE;
	}
	
	public static String TIGERTREEURNPREFIX = "urn:tree:tiger:";
	public static int TIGERTREEBASE32LENGTH = 39;
	
	/** Return true if the given URN is in the domain of this addressing scheme */ 
	public boolean wouldHandleUrn( String urn ) {
		return urn.startsWith(TIGERTREEURNPREFIX) || urn.startsWith(BitprintScheme.BITPRINTURNPREFIX);
	}

	/** Return the canonical identifier of the given blob */
	public byte[] getHash( Blob blob ) {
		return DigestUtil.digestBlob(blob, new TigerTree());
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
		return TIGERTREEURNPREFIX + Base32.encode(hash);
	}
	public byte[] urnToHash( String urn ) {
		String b32 = urn;
		if( urn.startsWith(TIGERTREEURNPREFIX) ) {
			b32 = urn.substring(TIGERTREEURNPREFIX.length(),TIGERTREEURNPREFIX.length()+TIGERTREEBASE32LENGTH);
		} else if( urn.startsWith(BitprintScheme.BITPRINTURNPREFIX) ) {
			b32 = urn.substring(BitprintScheme.BITPRINTURNPREFIX.length() + Sha1Scheme.SHA1BASE32LENGTH+1, BitprintScheme.BITPRINTURNPREFIX.length()+TIGERTREEBASE32LENGTH);
		} else {
			throw new BadlyFormedUrnException(urn);
		}
		if( b32.length() < TIGERTREEBASE32LENGTH ) throw new BadlyFormedUrnException(urn);
		return Base32.decode(b32);
	}
	
	// Convert to/from filename
	public String hashToFilename( byte[] hash ) {
		return Base32.encode(hash);
	}
	public byte[] filenameToHash( String filename ) {
		if( filename.length() < TIGERTREEBASE32LENGTH ) throw new BadlyFormedFilenameException(filename);
		return Base32.decode(filename);
	}
}
