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
	
	public static final String NS = "http://ns.nuke24.net/ContentCouch/";
	
	public static final String NAME             = NS + "name";
	public static final String TAG              = NS + "tag";
	public static final String COLLECTOR        = NS + "collector";
	public static final String IMPORTEDDATE     = NS + "importedDate";
	public static final String IMPORTEDFROM     = NS + "importedFrom";
	public static final String ENTRIES          = NS + "entries";
	/** What kind of object is target? */
	public static final String TARGETTYPE       = NS + "targetType";
	/** What is target? */
	public static final String TARGET           = NS + "target";
	public static final String SIZE             = NS + "size";
	/** If we can't directly represent target, link to its listing */
	public static final String TARGETLISTING    = NS + "targetListing";
	public static final String PARENT           = NS + "parent";

	public static final String HARDLINKABLE     = NS + "hardlinkable";
	public static final String BASE32SHA1       = NS + "base32Sha1";
	public static final String PARSED_FROM      = NS + "parsedFrom";

	public static final String DIRECTORY        = NS + "Directory";
	public static final String DIRECTORYENTRY   = NS + "DirectoryEntry";
	public static final String COMMIT           = NS + "Commit";
	public static final String REDIRECT         = NS + "Redirect";
	
	//// Request/response metadata ////
	
	public static final String RR_STORE_SECTOR     = NS + "RR/storeSector";
	public static final String RR_HARDLINK_DESIRED = NS + "RR/hardlinkDesired";
	public static final String RR_REHARDLINK_DESIRED = NS + "RR/rehardlinkDesired";
	
	public static final String RR_FILEMERGE_METHOD    = NS + "RR/fileMergeMethod";
	public static final String RR_FILEMERGE_FAIL      = "Fail";
	public static final String RR_FILEMERGE_REPLACE   = "Replace";
	public static final String RR_FILEMERGE_IGNORE    = "Ignore";
	public static final String RR_FILEMERGE_IFSAME    = "Same?";

	public static final String RR_DIRMERGE_METHOD     = NS + "RR/dirMergeMethod";
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
	
	static Map standardNsAbbreviations = new HashMap();
	static {
		standardNsAbbreviations.put("rdf", RdfNamespace.RDF_NS);
		standardNsAbbreviations.put("dc", DcNamespace.DC_NS);
		standardNsAbbreviations.put("ccouch", NS);
		standardNsAbbreviations.put("xmlns", "http://www.w3.org/2000/xmlns/");
		
		// Some other common namespaces:
		standardNsAbbreviations.put("xhtml", "http://www.w3.org/1999/xhtml");
		standardNsAbbreviations.put("svg", "http://www.w3.org/2000/svg");
		standardNsAbbreviations.put("xlink", "http://www.w3.org/1999/xlink");
		standardNsAbbreviations.put("foaf", "http://xmlns.com/foaf/0.1/");
	}
	public static final String STORE_SECTOR = RraNamespace.RRA_NS + "storeSector";
}
