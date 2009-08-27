package contentcouch.rdf;

import java.util.HashMap;
import java.util.Map;


public class CcouchNamespace {
	//// Types //// 
	
	public static final String URI_PARSE_PREFIX = "x-parse-rdf:";
	
	public static final String CC_NS = "http://ns.nuke24.net/ContentCouch/";
	public static final String INTERNAL_NS = CC_NS + "Internal/";
	public static final String BZ_NS = "http://bitzi.com/xmlns/2002/01/bz-core#";
	
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
	public static final String SHA1BASE32       = CC_NS + "sha1Base32";
	public static final String BITPRINT         = CC_NS + "bitprint";
	public static final String BASE32TIGERTREE  = BZ_NS + "fileTigerTree";
	public static final String PARSED_FROM      = CC_NS + "parsedFrom";

	public static final String DIRECTORY        = CC_NS + "Directory";
	public static final String DIRECTORYENTRY   = CC_NS + "DirectoryEntry";
	public static final String COMMIT           = CC_NS + "Commit";
	public static final String REDIRECT         = CC_NS + "Redirect";
	public static final String SOURCE_URI       = CC_NS + "sourceUri";
	
	//// Request metadata ////
	
	/** Namespace for ccouch-specific Request metadata */
	public static final String CC_REQ_NS = CC_NS + "Request/";
	
	public static final String REQ_LOCAL_REPOS_ONLY   = CC_REQ_NS + "searchLocalReposOnly";
	public static final String REQ_STORE_SECTOR       = CC_REQ_NS + "storeSector";
	public static final String REQ_HARDLINK_DESIRED   = CC_REQ_NS + "hardlinkDesired";
	public static final String REQ_SKIP_PREVIOUSLY_STORED_DIRS = CC_REQ_NS + "skipPreviouslyStoredDirectories";
	public static final String REQ_USE_URI_DOT_FILES  = CC_REQ_NS + "useUriDotFiles";
	public static final String REQ_CREATE_URI_DOT_FILES = CC_REQ_NS + "createUriDotFiles";
	public static final String REQ_DONT_CREATE_URI_DOT_FILES_WHEN_HIGHEST_BLOB_MTIME_GREATER_THAN =
		CC_REQ_NS + "dontCreateUriDotFilesWhenHighestBlobMtimeGreaterThan";
	
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
	public static final String RES_DEST_ALREADY_EXISTED = CC_RES_NS + "destinationAlreadyExisted";
	public static final String RES_HIGHEST_BLOB_MTIME = CC_RES_NS + "highestBlobModificationTime";

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
