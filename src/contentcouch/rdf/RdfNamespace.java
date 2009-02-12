package contentcouch.rdf;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import contentcouch.value.Ref;

public class RdfNamespace {
	//// Types //// 
	
	public static class Description extends RdfNode {
		public Ref about;
		public Description() {
			super(RDF_DESCRIPTION);
		}
	}

	//// Constants ////

	public static final String URI_PARSE_PREFIX = "x-parse-rdf:";
	
	public static final DateFormat CCOUCH_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String DC_NS  = "http://purl.org/dc/terms/";
	public static final String CCOUCH_NS = "http://ns.nuke24.net/ContentCouch/";
	
	public static final String RDF_ABOUT               = RDF_NS + "about";
	public static final String RDF_RESOURCE            = RDF_NS + "resource";
	public static final String RDF_PARSETYPE           = RDF_NS + "parseType";
	public static final String RDF_DESCRIPTION         = RDF_NS + "Description";
	
	public static final String DC_CREATOR              = DC_NS + "creator";
	public static final String DC_CREATED              = DC_NS + "created";
	public static final String DC_MODIFIED             = DC_NS + "modified";
	public static final String DC_FORMAT               = DC_NS + "format";

	public static final String DC_DESCRIPTION          = CCOUCH_NS + "description";
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
	
	public static final String CCOUCH_DIRECTORY        = CCOUCH_NS + "Directory";
	public static final String CCOUCH_DIRECTORYENTRY   = CCOUCH_NS + "DirectoryEntry";
	public static final String CCOUCH_COMMIT           = CCOUCH_NS + "Commit";
	public static final String CCOUCH_REDIRECT         = CCOUCH_NS + "Redirect";
	
	public static final String OBJECT_TYPE_BLOB = "Blob";
	public static final String OBJECT_TYPE_DIRECTORY = "Directory";
	public static final String OBJECT_TYPE_COMMIT = "Commit";
	/** Indicates a miscellaneous RDF structure */
	public static final String OBJECT_TYPE_RDF = "RDF";
	
	static Map standardNsAbbreviations = new HashMap();
	static {
		standardNsAbbreviations.put("rdf", RDF_NS);
		standardNsAbbreviations.put("dc", DC_NS);
		standardNsAbbreviations.put("ccouch", CCOUCH_NS);
		standardNsAbbreviations.put("xmlns", "http://www.w3.org/2000/xmlns/");
	}
}
