package contentcouch.rdf;

import java.util.HashMap;
import java.util.Map;

import togos.rra.RraNamespace;

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
	
	//// Request/response metadata ////
	
	/** Namespace for ccouch-specific Request/Response metadata */
	public static final String CCRR_NS = CC_NS + "RR/";
	
	public static final String RR_STORE_SECTOR       = CCRR_NS + "storeSector";
	public static final String RR_HARDLINK_DESIRED   = CCRR_NS + "hardlinkDesired";
	public static final String RR_REHARDLINK_DESIRED = CCRR_NS + "rehardlinkDesired";
	
	public static final String RR_FILEMERGE_METHOD    = CCRR_NS + "fileMergeMethod";
	public static final String RR_FILEMERGE_FAIL      = "Fail";
	public static final String RR_FILEMERGE_REPLACE   = "Replace";
	public static final String RR_FILEMERGE_IGNORE    = "Ignore";
	public static final String RR_FILEMERGE_IFSAME    = "Same?";
	public static final String RR_FILEMERGE_STRICTIG  = "Same?Ignore:Fail";
	public static final String RR_FILEMERGE_STRICTRE  = "Same?Replace:Fail";

	public static final String RR_DIRMERGE_METHOD     = CCRR_NS + "dirMergeMethod";
	public static final String RR_DIRMERGE_FAIL       = "Fail";
	public static final String RR_DIRMERGE_REPLACE    = "Replace";
	public static final String RR_DIRMERGE_IGNORE     = "Ignore";
	public static final String RR_DIRMERGE_MERGE      = "Merge";
	
	//// Object types ////
	
	public static final String OBJECT_TYPE_BLOB = "Blob";
	public static final String OBJECT_TYPE_DIRECTORY = "Directory";
	public static final String OBJECT_TYPE_COMMIT = "Commit";
	/** Indicates a miscellaneous RDF structure */
	public static final String OBJECT_TYPE_RDF = "RDF";
	
	//// Web page stuff ////

	public static final String WP_URI_WRAPPER = "WP/uriWrapper";
	
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
	public static final String STORE_SECTOR = RraNamespace.RRA_NS + "storeSector";
}
