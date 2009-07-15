package contentcouch.contentaddressing;

import contentcouch.value.Blob;

public interface ContentAddressingScheme {
	public static class BadlyFormedUrnException extends RuntimeException {
		public BadlyFormedUrnException(String badUrn) {
			super(badUrn);
		}
	} 
	
	public static class BadlyFormedFilenameException extends RuntimeException {
		public BadlyFormedFilenameException(String badFilename) {
			super(badFilename);
		}
	} 

	public static class BadlyFormedRdfValueException extends RuntimeException {
		public BadlyFormedRdfValueException(String badRdfValue) {
			super(badRdfValue);
		}
	} 

	
	
	public String getSchemeDisplayName();
	public String getRdfKey();
	
	/** Return true if the given URN is in the domain of this addressing scheme */ 
	public boolean wouldHandleUrn( String urn );

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
