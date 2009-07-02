package contentcouch.rdf;

import java.util.HashMap;
import java.util.Map;

import contentcouch.value.Ref;

public class CcouchNamespace {
	//// Types //// 
	
	public static class Description extends RdfNode {
		public Ref about;
		public Description() {
			super(RdfNamespace.RDF_DESCRIPTION);
		}
	}

	//// Constants ////

	public static final String URI_PARSE_PREFIX = "x-parse-rdf:";
	
	public static final String CC_NS = "http://ns.nuke24.net/ContentCouch/";
	public static final String INTERNAL_NS = CC_NS + "Internal/";
	
	public static final String NAME             = CC_NS + "name";
	public static final String TAG              = CC_NS + "tag";
	public static final String COLLECTOR        = CC_NS + "collector";
	public static final String IMPORTEDDATE     = CC_NS + "importedDate";
	public static final String IMPORTEDFROM     = CC_NS + "importedFrom";
	public static final String ENTRIES          = CC_NS + "entries";
	/** What kind of object is target? */
	public static final String TARGETTYPE       = CC_NS + "targetType";
	/** What is target? */
	public static final String TARGET           = CC_NS + "target";
	public static final String SIZE             = CC_NS + "size";
	/** If we can't directly represent target, link to its listing */
	public static final String TARGETLISTING    = CC_NS + "targetListing";
	public static final String PARENT           = CC_NS + "parent";

	public static final String HARDLINKABLE     = CC_NS + "hardlinkable";
	public static final String BASE32SHA1       = CC_NS + "base32Sha1";
	public static final String PARSED_FROM      = CC_NS + "parsedFrom";

	public static final String DIRECTORY        = CC_NS + "Directory";
	public static final String DIRECTORYENTRY   = CC_NS + "DirectoryEntry";
	public static final String COMMIT           = CC_NS + "Commit";
	public static final String REDIRECT         = CC_NS + "Redirect";
	public static final String SOURCE_URI       = CC_NS + "sourceUri";
	
	//// Request metadata ////
	
	/** Namespace for ccouch-specific Request metadata */
	public static final String CC_REQ_NS = CC_NS + "Request/";
	
	public static final String REQ_STORE_SECTOR       = CC_REQ_NS + "storeSector";
	public static final String REQ_HARDLINK_DESIRED   = CC_REQ_NS + "hardlinkDesired";
	public static final String REQ_REHARDLINK_DESIRED = CC_REQ_NS + "rehardlinkDesired";
	
	public static final String REQ_FILEMERGE_METHOD    = CC_REQ_NS + "fileMergeMethod";
	public static final String REQ_FILEMERGE_FAIL      = "Fail";
	public static final String REQ_FILEMERGE_REPLACE   = "Replace";
	public static final String REQ_FILEMERGE_IGNORE    = "Ignore";
	public static final String REQ_FILEMERGE_IFSAME    = "Same?";
	public static final String REQ_FILEMERGE_STRICTIG  = "Same?Ignore:Fail";
	public static final String REQ_FILEMERGE_STRICTRE  = "Same?Replace:Fail";

	public static final String REQ_DIRMERGE_METHOD     = CC_REQ_NS + "dirMergeMethod";
	public static final String REQ_DIRMERGE_FAIL       = "Fail";
	public static final String REQ_DIRMERGE_REPLACE    = "Replace";
	public static final String REQ_DIRMERGE_IGNORE     = "Ignore";
	public static final String REQ_DIRMERGE_MERGE      = "Merge";
	
	public static final String REQ_STORE_SIMPLE_DIRS   = CC_REQ_NS + "storeSimpleDirs";
	
	//public static final String RR_DESIRED_STORE_SECTOR = CC_REQ_NS + "desiredStoreSector";
	
	//// Response metadata ////
	
	public static final String CC_RES_NS = CC_NS + "Response/";
	public static final String RES_STORED_IDENTIFIER = CC_RES_NS + "storedIdentifier";
	public static final String RES_STORED_OBJECT = CC_RES_NS + "storedObject";
	public static final String RES_CACHEABLE = CC_RES_NS + "cacheable";

	//// Object types ////
	
	public static final String OBJECT_TYPE_BLOB = "Blob";
	public static final String OBJECT_TYPE_DIRECTORY = "Directory";
	public static final String OBJECT_TYPE_COMMIT = "Commit";
	/** Indicates a miscellaneous RDF structure */
	public static final String OBJECT_TYPE_RDF = "RDF";
	
	//// XML Namespaces ////
	
	static Map standardNsAbbreviations = new HashMap();
	static {
		standardNsAbbreviations.put("rdf", RdfNamespace.RDF_NS);
		standardNsAbbreviations.put("dc", DcNamespace.DC_NS);
		standardNsAbbreviations.put("ccouch", CC_NS);
		standardNsAbbreviations.put("xmlns", "http://www.w3.org/2000/xmlns/");
		
		// Some other common namespaces:
		standardNsAbbreviations.put("xhtml", "http://www.w3.org/1999/xhtml");
		standardNsAbbreviations.put("svg", "http://www.w3.org/2000/svg");
		standardNsAbbreviations.put("xlink", "http://www.w3.org/1999/xlink");
		standardNsAbbreviations.put("foaf", "http://xmlns.com/foaf/0.1/");
	}
}
