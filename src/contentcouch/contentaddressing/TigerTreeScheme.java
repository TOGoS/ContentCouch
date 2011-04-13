package contentcouch.contentaddressing;

import java.util.Arrays;

import org.bitpedia.util.Base32;
import org.bitpedia.util.TigerTree;

import contentcouch.blob.Blob;
import contentcouch.digest.DigestUtil;
import contentcouch.rdf.BitziNamespace;

public class TigerTreeScheme implements ContentAddressingScheme {
	public static final TigerTreeScheme instance = new TigerTreeScheme();
	public static final TigerTreeScheme getInstance() { return instance; }
	
	public static String TIGERTREEURNPREFIX = "urn:tree:tiger:";
	public static int TIGERTREEHASHLENGTH = 24;
	public static int TIGERTREEBASE32LENGTH = 39;	
	
	public String getSchemeDisplayName() {
		return "Base32-encoded Tiger-Tree";
	}
	public String getSchemeShortName() {
		return "tigertree";
	}
	public String getRdfKey() {
		return BitziNamespace.BZ_BASE32TIGERTREE;
	}
	
	public int getHashLength() {
		return TIGERTREEHASHLENGTH;
	}
	
	public boolean couldTranslateUrn( String urn ) {
		return urn.startsWith(TIGERTREEURNPREFIX) || urn.startsWith(BitprintScheme.BITPRINTURNPREFIX);
	}
	public boolean canVerifyUrn( String urn ) {
		return urn.startsWith(TIGERTREEURNPREFIX) || urn.startsWith(BitprintScheme.BITPRINTURNPREFIX);
	}
	public boolean couldGenerateUrn( String urn ) {
		return urn.startsWith(TIGERTREEURNPREFIX);
	}
	
	public boolean verify( String urn, Blob blob ) {
		return Arrays.equals( urnToHash(urn), getHash(blob) );
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
		if( value.length() < 32 ) throw new BadlyFormedRdfValueException(value, "Wrong length");
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
			throw new BadlyFormedUrnException(urn, "URI not handled by this scheme");
		}
		if( b32.length() < TIGERTREEBASE32LENGTH ) throw new BadlyFormedUrnException(urn, "Wrong length");
		return Base32.decode(b32);
	}
	
	// Convert to/from filename
	public String hashToFilename( byte[] hash ) {
		return Base32.encode(hash);
	}
	public byte[] filenameToHash( String filename ) {
		if( filename.length() < TIGERTREEBASE32LENGTH ) throw new BadlyFormedFilenameException(filename, "Wrong length");
		return Base32.decode(filename);
	}
}
