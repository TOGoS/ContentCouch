package contentcouch.rdf;

import java.util.HashMap;
import java.util.Map;


public class CCouchNamespace {
	//// Types //// 
	
	public static final String RDF_SUBJECT_URI_PREFIX = "x-rdf-subject:";
	public static final String RDF_SUBJECT_URI_PREFIX_OLD = "x-parse-rdf:";
	public static final String[] RDF_SUBJECT_URI_PREFIXES = {
		RDF_SUBJECT_URI_PREFIX, RDF_SUBJECT_URI_PREFIX_OLD
	};
	
	public static final String CC_NS = "http://ns.nuke24.net/ContentCouch/";
	public static final String CTX_NS = CC_NS + "Context/";
	public static final String CFG_NS = CC_NS + "Config/";
	
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
	/** Old way to specify file size - use bz:fileLength for new-style things */
	public static final String SIZE             = CC_NS + "size";
	/** If we can't directly represent target, link to its listing */
	public static final String TARGETLISTING    = CC_NS + "targetListing";
	public static final String PARENT           = CC_NS + "parent";

	public static final String HARDLINKABLE     = CC_NS + "hardlinkable";
	public static final String SHA1BASE32       = CC_NS + "sha1Base32";
	public static final String BITPRINT         = CC_NS + "bitprint";
	public static final String PARSED_FROM      = CC_NS + "parsedFrom";

	public static final String BLOB             = CC_NS + "Blob";
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
	public static final String REQ_CACHE_SECTOR       = CC_REQ_NS + "cacheSector";
	public static final String REQ_DONT_CACHE_FILE_HASHES = CC_REQ_NS + "dontCacheFileHashes";
	public static final String REQ_HARDLINK_DESIRED   = CC_REQ_NS + "hardlinkDesired";
	/** When caching, skip directories that are marked as already fully stored.
	 * Defaults to true */
	public static final String REQ_SKIP_PREVIOUSLY_STORED_DIRS = CC_REQ_NS + "skipPreviouslyStoredDirectories";
	public static final String REQ_USE_URI_DOT_FILES  = CC_REQ_NS + "useUriDotFiles";
	public static final String REQ_CREATE_URI_DOT_FILES = CC_REQ_NS + "createUriDotFiles";
	public static final String REQ_DONT_CREATE_URI_DOT_FILES_WHEN_HIGHEST_BLOB_MTIME_GREATER_THAN =
		CC_REQ_NS + "dontCreateUriDotFilesWhenHighestBlobMtimeGreaterThan";
	/** Accomplishes the same thing as uri dot files, but uses the same
	 * filename,mtime -> hash database as used for files. */
	public static final String REQ_CACHE_DIRECTORY_HASHES = CC_REQ_NS + "cacheDirectoryHashes";
	
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
	
	/* When merging directories, copy directories that are new to to the destination
	 * rather than referencing source directories directly */
	public static final String REQ_COPY_SOURCE_DIRS    = CC_REQ_NS + "copySourceDirectories";
	
	//public static final String RR_DESIRED_STORE_SECTOR = CC_REQ_NS + "desiredStoreSector";
	
	public static final String CFG_RDF_SUBJECT_URI_PREFIX = CFG_NS + "rdfSubjectUriPrefix";
	public static final String CFG_RDF_DIRECTORY_STYLE = CFG_NS + "directoryStyle";
	/** The 'short name' of a scheme (see Schemes) */
	public static final String CFG_ID_SCHEME = CFG_NS + "blobIdentificationScheme";
	
	/* Defaults to true */
	public static final String REQ_CACHE_COMMIT_TARGETS = CC_REQ_NS + "cacheCommitTargets";
	/* Should be an integer specifying how many levels of commit ancestors to cache
	 * when a commit object is pushed into the repository. */
	public static final String REQ_CACHE_COMMIT_ANCESTORS = CC_REQ_NS + "cacheCommitAncestors";
	
	//// Response metadata ////
	
	public static final String CC_RES_NS = CC_NS + "Response/";
	public static final String RES_STORED_IDENTIFIER = CC_RES_NS + "storedIdentifier";
	public static final String RES_TREE_FULLY_STORED = CC_RES_NS + "treeFullyStored";
	public static final String RES_STORED_OBJECT = CC_RES_NS + "storedObject";
	public static final String RES_CACHEABLE = CC_RES_NS + "cacheable";
	public static final String RES_DEST_ALREADY_EXISTED = CC_RES_NS + "destinationAlreadyExisted";
	public static final String RES_HIGHEST_BLOB_MTIME = CC_RES_NS + "highestBlobModificationTime";
	/** If a requested URI is equivalent to a simpler one and resolved using that,
	 * the simpler one should be returned here */
	public static final String RES_RESOLVED_URI = CC_RES_NS + "resolvedUri";

	//// Object types ////
	
	/*
	 * In old-style RDF, these are used to indicate types of
	 * objects referenced from directory entries using <targetType>.
	 * 
	 * Transitioning to new style, these should be converted
	 * to the fully namespaced versions.  e.g.
	 * 
	 *  <targetType>Blob</targetType> should be interpreted like
	 *  <target><Blob>...</Blob></target>
	 *  
	 *  where <Blob> means thing with <rdf:type rdf:resource="http://ns.nuke24.net/ContentCouch/Blob"/>
	 */
	
	public static final String TT_SHORTHAND_BLOB = "Blob";
	public static final String TT_SHORTHAND_DIRECTORY = "Directory";
	
	public static final String RDF_DIRECTORY_STYLE_NEW = "2";
	public static final String RDF_DIRECTORY_STYLE_OLD = "1";
	
	public static final String ID_SCHEME_DEFAULT = "bitprint";

	//// XML Namespaces ////
	
	static Map standardNsAbbreviations = new HashMap();
	static {
		standardNsAbbreviations.put("rdf", RdfNamespace.RDF_NS);
		standardNsAbbreviations.put("dc", DcNamespace.DCTERMS_NS);
		standardNsAbbreviations.put("bz", BitziNamespace.BZ_NS);
		standardNsAbbreviations.put("ccouch", CC_NS);
		standardNsAbbreviations.put("xmlns", "http://www.w3.org/2000/xmlns/");
		
		// Some other common namespaces:
		standardNsAbbreviations.put("xhtml", "http://www.w3.org/1999/xhtml");
		standardNsAbbreviations.put("svg", "http://www.w3.org/2000/svg");
		standardNsAbbreviations.put("xlink", "http://www.w3.org/1999/xlink");
		standardNsAbbreviations.put("foaf", "http://xmlns.com/foaf/0.1/");
	}

	static Map newStandardNsAbbreviations = new HashMap();
	static {
		newStandardNsAbbreviations.put("rdf", RdfNamespace.RDF_NS);
		newStandardNsAbbreviations.put("dcterms", DcNamespace.DCTERMS_NS);
		newStandardNsAbbreviations.put("bz", BitziNamespace.BZ_NS);
		newStandardNsAbbreviations.put("ccouch", CC_NS);
		newStandardNsAbbreviations.put("xmlns", "http://www.w3.org/2000/xmlns/");
		
		// Some other common namespaces:
		newStandardNsAbbreviations.put("xhtml", "http://www.w3.org/1999/xhtml");
		newStandardNsAbbreviations.put("svg", "http://www.w3.org/2000/svg");
		newStandardNsAbbreviations.put("xlink", "http://www.w3.org/1999/xlink");
		newStandardNsAbbreviations.put("foaf", "http://xmlns.com/foaf/0.1/");
	}
}
