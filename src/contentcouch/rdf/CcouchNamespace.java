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
	
	public static final String CCOUCH_NS = "http://ns.nuke24.net/ContentCouch/";
	
	public static final String CCOUCH_NAME             = CCOUCH_NS + "name";
	public static final String CCOUCH_TAG              = CCOUCH_NS + "tag";
	public static final String CCOUCH_COLLECTOR        = CCOUCH_NS + "collector";
	public static final String CCOUCH_IMPORTEDDATE     = CCOUCH_NS + "importedDate";
	public static final String CCOUCH_IMPORTEDFROM     = CCOUCH_NS + "importedFrom";
	public static final String CCOUCH_ENTRIES          = CCOUCH_NS + "entries";
	/** What kind of object is target? */
	public static final String CCOUCH_TARGETTYPE       = CCOUCH_NS + "targetType";
	/** What is target? */
	public static final String CCOUCH_TARGET           = CCOUCH_NS + "target";
	public static final String CCOUCH_SIZE             = CCOUCH_NS + "size";
	/** If we can't directly represent target, link to its listing */
	public static final String CCOUCH_TARGETLISTING    = CCOUCH_NS + "targetListing";
	public static final String CCOUCH_PARENT           = CCOUCH_NS + "parent";

	public static final String CCOUCH_HARDLINKABLE     = CCOUCH_NS + "hardlinkable";
	public static final String CCOUCH_BASE32SHA1       = CCOUCH_NS + "base32Sha1";
	public static final String CCOUCH_PARSED_FROM      = CCOUCH_NS + "parsedFrom";

	public static final String CCOUCH_DIRECTORY        = CCOUCH_NS + "Directory";
	public static final String CCOUCH_DIRECTORYENTRY   = CCOUCH_NS + "DirectoryEntry";
	public static final String CCOUCH_COMMIT           = CCOUCH_NS + "Commit";
	public static final String CCOUCH_REDIRECT         = CCOUCH_NS + "Redirect";
	
	// Request/response metadata
	
	public static final String CCOUCH_RRA_STORE_SECTOR     = CCOUCH_NS + "RRA/storeSector";
	public static final String CCOUCH_RRA_HARDLINK_DESIRED = CCOUCH_NS + "RRA/hardlinkDesired";
	public static final String CCOUCH_RRA_HTTP_DIRECTORIES_DESIRED = CCOUCH_NS + "RRA/httpDirectoriesDesired";
	
	public static final String OBJECT_TYPE_BLOB = "Blob";
	public static final String OBJECT_TYPE_DIRECTORY = "Directory";
	public static final String OBJECT_TYPE_COMMIT = "Commit";
	/** Indicates a miscellaneous RDF structure */
	public static final String OBJECT_TYPE_RDF = "RDF";
	
	static Map standardNsAbbreviations = new HashMap();
	static {
		standardNsAbbreviations.put("rdf", RdfNamespace.RDF_NS);
		standardNsAbbreviations.put("dc", DcNamespace.DC_NS);
		standardNsAbbreviations.put("ccouch", CCOUCH_NS);
		standardNsAbbreviations.put("xmlns", "http://www.w3.org/2000/xmlns/");
	}
	public static final String STORE_SECTOR = RraNamespace.RRA_NS + "storeSector";
}
