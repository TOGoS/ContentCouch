package contentcouch.contentaddressing;

import togos.mf.value.Blob;

public interface ContentAddressingScheme {
	public static class SomethingIsBadlyFormedException extends RuntimeException {
		public SomethingIsBadlyFormedException( String somethingBadlyFormed, String explain ) {
			super(explain + ": " + somethingBadlyFormed);
		}
	}
	
	public static class BadlyFormedUrnException extends SomethingIsBadlyFormedException {
		public BadlyFormedUrnException( String badUrn, String explain ) {
			super( badUrn, explain );
		}
	} 
	
	public static class BadlyFormedFilenameException extends SomethingIsBadlyFormedException {
		public BadlyFormedFilenameException( String badFilename, String explain ) {
			super( badFilename, explain );
		}
	} 

	public static class BadlyFormedRdfValueException extends SomethingIsBadlyFormedException {
		public BadlyFormedRdfValueException( String badRdfValue, String explain ) {
			super( badRdfValue, explain );
		}
	}

	
	
	public String getSchemeDisplayName();
	public String getSchemeShortName();
	public String getRdfKey();
	/** Number of bytes in a hash */
	public int getHashLength();
	
	/** Return true if the given URN can be translated to this scheme */
	public boolean couldTranslateUrn( String urn );
	/** Return true if we can verify a blob given the given URN */
	public boolean canVerifyUrn( String urn );
	/** Return true if the given URN is in the output range of this addressing scheme */ 
	public boolean couldGenerateUrn( String urn );
	
	public boolean verify( String urn, Blob blob );
	
	/** Return the canonical identifier of the given blob */
	public byte[] getHash( Blob blob );

	// Convert to/from RDF value
	public String hashToRdfValue( byte[] hash );
	public byte[] rdfValueToHash( String value );

	// Convert to/from URN
	public String hashToUrn( byte[] hash );
	public byte[] urnToHash( String urn );

	// Convert to/from filename
	public String hashToFilename( byte[] hash );
	public byte[] filenameToHash( String filename );
}
