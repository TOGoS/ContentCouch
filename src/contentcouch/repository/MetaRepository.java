package contentcouch.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import togos.mf.value.Blob;

import com.eekboom.utils.Strings;

import contentcouch.activefunctions.FollowPath;
import contentcouch.app.Log;
import contentcouch.blob.BlobUtil;
import contentcouch.contentaddressing.ContentAddressingScheme;
import contentcouch.contentaddressing.Schemes;
import contentcouch.directory.WritableDirectory;
import contentcouch.file.FileBlob;
import contentcouch.file.FileUtil;
import contentcouch.framework.BaseRequestHandler;
import contentcouch.framework.MfArgUtil;
import contentcouch.hashcache.FileHashCache;
import contentcouch.hashcache.SimpleListFile;
import contentcouch.misc.Function1;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.SimpleDirectory;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.rdf.DcNamespace;
import contentcouch.rdf.RdfCommit;
import contentcouch.rdf.RdfDirectory;
import contentcouch.rdf.RdfIO;
import contentcouch.rdf.RdfNode;
import contentcouch.store.TheGetter;
import contentcouch.value.BaseRef;
import contentcouch.value.Commit;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class MetaRepository extends BaseRequestHandler {
	public static class RepoRef {
		public String repoName, subPath;
		
		public RepoRef( String repoName, String subPath ) {
			this.repoName = repoName;
			this.subPath = subPath;
		}
		
		public String getHeadPath() {
			if( subPath.startsWith("heads/") ) return subPath.substring("heads/".length());
			return null;
		}
		
		/**
		 * Parses [x-ccouch-{repo|head}:][//reponame][/]stuff into RepoRefs.
		 * If reponame is not given, return repoRef.repoName will be null, indicating 'default'
		 * @param uri string to parse
		 * @param assumeHead when no scheme explicitly given, treat as x-ccouch-head URI
		 * @return
		 */
		public static RepoRef parse(String uri, boolean assumeHead) {
			String repoName = null;
			if( uri.startsWith("x-ccouch-head:") ) {
				uri = uri.substring("x-ccouch-head:".length());
				assumeHead = true;
			}
			if( uri.startsWith("x-ccouch-repo:") ) {
				uri = uri.substring("x-ccouch-repo:".length());
				assumeHead = false;
			}
			if( uri.startsWith("//") ) {
				uri = uri.substring(2);
				int nextSlash = uri.indexOf('/');
				if( nextSlash == -1 ) {
					// Not a well-formed URI, but we'll let it slide...
					repoName = uri;
					uri = "";
				} else {
					repoName = uri.substring(0,nextSlash);
					uri = uri.substring(nextSlash+1);
				}
			}
			if( uri.startsWith("/") ) uri = uri.substring(1);
			return new RepoRef(repoName, assumeHead ? "heads/" + uri : uri );
		}
		
		public String toString() {
			return "x-ccouch-repo://" + repoName + "/" + subPath;
		}
	}

	protected String filenameToPostSectorPath( RepoConfig repoConfig, String filename ) {
		if( filename.length() >= 2 ) {
			return filename.substring(0,2) + "/" + filename;
		} else {
			return filename;
		}
	}
	
	protected Map fileHashCaches = new HashMap();
	protected FileHashCache getFileHashCache( ContentAddressingScheme cas ) {
		FileHashCache fileHashCache = (FileHashCache)fileHashCaches.get(cas.getSchemeShortName());
		if( fileHashCache == null && config.defaultRepoConfig.uri.startsWith("file:") ) {
			String hashCachePath = PathUtil.parseFilePathOrUri(config.defaultRepoConfig.uri + "cache/file-"+cas.getSchemeShortName()+".slf").toString();
			File cacheFile = new File(hashCachePath);
			fileHashCache = new FileHashCache(cacheFile, cas, "rw");
			fileHashCaches.put(cas.getSchemeShortName(), fileHashCache);
		}
		return fileHashCache;
	}
	
	protected String urnToPostSectorPath( RepoConfig repoConfig, String urn ) {
		if( !repoConfig.storageScheme.couldTranslateUrn(urn) ) return null;
		byte[] hash = repoConfig.storageScheme.urnToHash(urn);
		String filename = repoConfig.storageScheme.hashToFilename(hash);
		return filenameToPostSectorPath(repoConfig, filename);
	}
	
	protected List getRepoDataSectorUrls( RepoConfig repoConfig ) {
		ArrayList l = new ArrayList();
		String dataDirUri = PathUtil.appendPath(repoConfig.uri,"data/");
		
		BaseRequest dirReq = new BaseRequest( RequestVerbs.VERB_GET, dataDirUri );
		Response dirRes = TheGetter.call(dirReq);
		if( dirRes.getStatus() != ResponseCodes.RESPONSE_NORMAL ) return l;
		if( !(dirRes.getContent() instanceof Directory) ) {
			Log.log(Log.EVENT_WARNING, dataDirUri + " exists but is not a directory");
		}
		Directory d = (Directory)dirRes.getContent();
		for( Iterator i=d.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry e = (Directory.Entry)i.next();
			if( CcouchNamespace.TT_SHORTHAND_DIRECTORY.equals(e.getTargetType()) ) {
				l.add(PathUtil.appendPath(dataDirUri, e.getName() + "/"));
			}
		}
		return l;
	}
	
	Pattern entryKeyNumberExtractionPattern = Pattern.compile("^(\\d+).*$");
	
	protected String getHighestEntryKey( String uri ) {
		Directory d = TheGetter.getDirectory(uri);
		if( d == null ) return null;
		String highest = null;
		for( Iterator i=d.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry e = (Directory.Entry)i.next();
			if( entryKeyNumberExtractionPattern.matcher(e.getName()).matches() ) {
				if( highest == null ) {
					highest = e.getName();
				} else {
					if( Strings.compareNatural(e.getName(), highest) > 0 ) {
						highest = e.getName();
					}
				}
			}
		}
		return highest;
	}
	
	protected String getNextEntryKey( String uri ) {
		String highest = getHighestEntryKey(uri);
		if( highest == null ) return "0";
		Matcher m = entryKeyNumberExtractionPattern.matcher(highest);
		if( m.matches() ) { // It'd better!
			long l = Long.parseLong(m.group(1));
			return String.valueOf(l+1);
		} else {
			throw new RuntimeException("Somehow found a highest key that is not numeric!");
		}
	}
	
	/**
	 * @param repoConfig the RepoConfig for the repository we are looking for heads in
	 * @param headPath the part of the URI *after* "x-ccouch-head:/" or "x-ccouch-repo:.../heads/",
	 *   e.g. "someserver/someproject/latest"
	 * @param defaultBaseName if headPath ends with /, act like th
	 * @return the URI to the backing resource
	 */
	protected String resolveHeadPath( RepoConfig repoConfig, String headPath, String defaultBaseName ) {
		String path = "heads/"+headPath;
		
		int dLast = path.lastIndexOf('/');
		String baseName = path.substring(dLast+1);
		if( baseName.length() == 0 ) {
			baseName = defaultBaseName;
		}
		String dirPath = repoConfig.uri + path.substring(0,dLast+1);
		if( "new".equals(baseName) ) {
			return dirPath + getNextEntryKey(dirPath); 
		} else if( "latest".equals(baseName) ) {
			return dirPath + getHighestEntryKey(dirPath);
		} else {
			return repoConfig.uri + path;
		}
	}

	protected String getRequestedStoreSector( Request req, RepoConfig repoConfig ) {
		String ss = ValueUtil.getString(req.getMetadata().get(CcouchNamespace.REQ_STORE_SECTOR));
		if( ss == null ) ss = repoConfig.userStoreSector;
		return ss;
	}
	
	//// PUT stuff ////
	
	// request verb and URI will be ignored for all put* methods
	
	protected Response putDataBlob( RepoConfig repoConfig, Request req ) {
		String sector = getRequestedStoreSector(req, repoConfig);
		
		String idUrn = identifyBlob((Blob)req.getContent(), repoConfig);
		
		byte[] storeHash;
		if( repoConfig.storageScheme.couldTranslateUrn(idUrn) ) {
			storeHash = repoConfig.storageScheme.urnToHash(idUrn);
		} else {
			storeHash = repoConfig.storageScheme.getHash((Blob)req.getContent());
		}
		
		String filename = repoConfig.storageScheme.hashToFilename(storeHash);
		String psp = filenameToPostSectorPath(repoConfig, filename);
		String uri = repoConfig.uri + "data/" + sector + "/" + psp; 
		
		BaseRequest subReq = new BaseRequest( req, uri );
		subReq.verb = RequestVerbs.VERB_PUT;
		BaseResponse res = new BaseResponse( TheGetter.call(subReq) );
		res.putMetadata(CcouchNamespace.RES_STORED_IDENTIFIER, idUrn);
		Date mtime = (Date)req.getContentMetadata().get(DcNamespace.DC_MODIFIED);
		if( mtime != null ) {
			res.putMetadata(CcouchNamespace.RES_HIGHEST_BLOB_MTIME, mtime);
		}
		return res;
	}

	protected Response putDataRdf( RepoConfig repoConfig, Request req ) {
		Object parsedFrom = req.getContentMetadata().get(CcouchNamespace.PARSED_FROM);
		String sourceUri = (String)req.getContentMetadata().get(CcouchNamespace.SOURCE_URI);
		BaseRequest subReq = new BaseRequest();
		subReq.metadata = req.getMetadata();
		String rdfString = ((RdfNode)req.getContent()).toString();
		if( rdfString.charAt(rdfString.length()-1) != '\n' ) rdfString += "\n";
		subReq.content = parsedFrom != null ? parsedFrom : BlobUtil.getBlob(rdfString);
		if( sourceUri != null ) {
			subReq.putContentMetadata(CcouchNamespace.SOURCE_URI, "x-rdfified:" + sourceUri);
		}
		Response blobPutRes = putDataBlob( repoConfig, subReq );
		if( blobPutRes.getStatus() != ResponseCodes.RESPONSE_NORMAL ) return blobPutRes;
		BaseResponse res = new BaseResponse();
		MetadataUtil.copyStoredIdentifier(blobPutRes, res, CcouchNamespace.RDF_SUBJECT_URI_PREFIX);
		return res;
	}
	
	// TODO: finish implementing this stuff and do away with old 'put' methods
	protected Response putDataRdfDirectory( RepoConfig repoConfig, Request req ) {
		Response putRdfBlobRes = putDataRdf( repoConfig, req );
		TheGetter.getResponseValue(putRdfBlobRes, req);
		
		// TODO: Base 'has this been stored already' on something more definitive than that
		// the blobbified RDF is in the datastore.  It may have gotten there other ways
		// (during cache-on-GET, or by simply caching the blob and not its parsed RDF).
		if( MetadataUtil.isEntryTrue(req.getMetadata(), CcouchNamespace.REQ_SKIP_PREVIOUSLY_STORED_DIRS) &&
		    MetadataUtil.isEntryTrue(req.getMetadata(), CcouchNamespace.RES_DEST_ALREADY_EXISTED) ) {
			BaseResponse res = new BaseResponse(ResponseCodes.RESPONSE_NORMAL, "Rdf directory already stored - skipping", "text/plain");
			res.putMetadata(CcouchNamespace.RES_DEST_ALREADY_EXISTED, Boolean.TRUE);
			MetadataUtil.copyStoredIdentifier(putRdfBlobRes, res, null);
			return res;
		}

		Directory d = (Directory)req.getContent();
		for( Iterator i=d.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry e = (Directory.Entry)i.next();
			Object target = e.getTarget();
			BaseRequest targetPutReq = new BaseRequest();
			MetadataUtil.dereferenceTargetToRequest( target, targetPutReq );
			targetPutReq.metadata = req.getMetadata();
			putData( repoConfig, targetPutReq );
		}
		
		BaseResponse res = new BaseResponse(ResponseCodes.RESPONSE_NORMAL, "Rdf directory and entries stored", "text/plain");
		MetadataUtil.copyStoredIdentifier(putRdfBlobRes, res, null);
		return res;
	}
	
	protected Response putDataNonRdfDirectory( RepoConfig repoConfig, Request req ) {
		RdfDirectory rdfDir = new RdfDirectory();
		
		Directory d = (Directory)req.getContent();
		
		if( MetadataUtil.isEntryTrue(req.getMetadata(), CcouchNamespace.REQ_USE_URI_DOT_FILES) ) {
			Directory.Entry uriDotFileEntry = d.getDirectoryEntry(".ccouch-uri");
			if( uriDotFileEntry != null ) {
				Object target = uriDotFileEntry.getTarget();
				if( target instanceof Ref ) target = TheGetter.get( ((Ref)target).getTargetUri() );
				String uri = ValueUtil.getString(target);
				// TODO: Some kind of validation on the URI (should be x-rdf-subject:<urn scheme>:...)
				BaseResponse res = new BaseResponse(ResponseCodes.RESPONSE_NORMAL, "URI previously cached in .ccouch-uri file - skipping", "text/plain");
				res.putMetadata(CcouchNamespace.RES_STORED_IDENTIFIER, uri);
				return res;
			}
		}		
		String sourceUri = (String)req.getContentMetadata().get(CcouchNamespace.SOURCE_URI);		
		//(Date)req.getContentMetadata().get(DcNamespace.DC_MODIFIED);
		Date highestMtime = null; // Don't pay attention to directory mtime
		List rdfDirectoryEntries = new ArrayList();
		for( Iterator i=d.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry e = (Directory.Entry)i.next();
			Object target = e.getTarget();
			String targetSourceUri;
			if( target instanceof Ref ) {
				targetSourceUri = ((Ref)target).getTargetUri();
				BaseRequest targetGetReq = new BaseRequest( "GET", targetSourceUri );
				targetGetReq.metadata = req.getMetadata();
				targetGetReq.contextVars = req.getContextVars();
				target = TheGetter.getResponseValue( TheGetter.call( targetGetReq ), targetGetReq );
			} else {
				if( sourceUri != null ) {
					targetSourceUri = PathUtil.appendPath(sourceUri, e.getName(), false);
				} else {
					targetSourceUri = null;
				}
			}
			BaseRequest targetPutReq = new BaseRequest();
			targetPutReq.content = target;
			targetPutReq.metadata = req.getMetadata();
			targetPutReq.contextVars = req.getContextVars();
			targetPutReq.putContentMetadata(CcouchNamespace.SOURCE_URI, targetSourceUri);
			long entryMtime = e.getTargetLastModified();
			if( entryMtime != -1 ) {
				targetPutReq.putContentMetadata(DcNamespace.DC_MODIFIED, new Date(entryMtime));
			}
			Response targetPutRes = putData( repoConfig, targetPutReq );
			TheGetter.getResponseValue(targetPutRes, targetPutReq);
			Date subHighestMtime = (Date)targetPutRes.getMetadata().get(CcouchNamespace.RES_HIGHEST_BLOB_MTIME);
			if( subHighestMtime != null && (highestMtime == null || subHighestMtime.compareTo(highestMtime) > 0) ) {
				highestMtime = subHighestMtime;
			}
			String targetUri = MetadataUtil.getStoredIdentifier(targetPutRes);
			if( targetUri == null ) throw new RuntimeException("Inserting entry target returned null");
			rdfDirectoryEntries.add(new RdfDirectory.Entry(e, new BaseRef(targetUri)));
		}
		rdfDir.setDirectoryEntries(rdfDirectoryEntries);
		
		BaseRequest dirPutReq = new BaseRequest();
		dirPutReq.content = rdfDir;
		dirPutReq.contentMetadata = req.getContentMetadata();
		dirPutReq.metadata = req.getMetadata();
		BaseResponse dirPutRes = new BaseResponse(putDataRdf( repoConfig, dirPutReq ));
		if( highestMtime != null ) {
			dirPutRes.putMetadata(CcouchNamespace.RES_HIGHEST_BLOB_MTIME, highestMtime);
		}

		boolean oldEnough;
		if( highestMtime == null ) {
			oldEnough = true; // Meh?
		} else {
			Date noNewerThan = (Date)req.getMetadata().get(CcouchNamespace.REQ_DONT_CREATE_URI_DOT_FILES_WHEN_HIGHEST_BLOB_MTIME_GREATER_THAN);
			oldEnough = (noNewerThan == null) ? true : highestMtime.before(noNewerThan); 
		}
		
		String storedDirUri = MetadataUtil.getStoredIdentifier(dirPutRes);
		if( d instanceof WritableDirectory &&
			MetadataUtil.isEntryTrue(req.getMetadata(),CcouchNamespace.REQ_CREATE_URI_DOT_FILES) &&
			oldEnough &&
			storedDirUri != null
		) {
			MetadataUtil.saveCcouchUri( (WritableDirectory)d, storedDirUri );
		}

		return dirPutRes;
	}

	protected Response putDataRdfCommit( RepoConfig repoConfig, Request req ) {
		Response putRdfBlobRes = putDataRdf( repoConfig, req );
		TheGetter.getResponseValue(putRdfBlobRes, req);
		
		// TODO: Base 'has this been stored already' on something more definitive than that
		// the blobbified RDF is in the datastore.  It may have gotten there other ways
		// (during cache-on-GET, or by simply caching the blob and not its parsed RDF).
		if( MetadataUtil.isEntryTrue(req.getMetadata(), CcouchNamespace.REQ_SKIP_PREVIOUSLY_STORED_DIRS) &&
		    MetadataUtil.isEntryTrue(req.getMetadata(), CcouchNamespace.RES_DEST_ALREADY_EXISTED) ) {
			BaseResponse res = new BaseResponse(ResponseCodes.RESPONSE_NORMAL, "Rdf commit already stored - skipping", "text/plain");
			res.putMetadata(CcouchNamespace.RES_DEST_ALREADY_EXISTED, Boolean.TRUE);
			MetadataUtil.copyStoredIdentifier(putRdfBlobRes, res, null);
			return res;
		}

		Commit c = (Commit)req.getContent();
		Object target = c.getTarget();
		BaseRequest targetPutReq = new BaseRequest();
		targetPutReq.metadata = req.getMetadata();
		MetadataUtil.dereferenceTargetToRequest( target, targetPutReq );
		putData( repoConfig, targetPutReq );
		
		BaseResponse res = new BaseResponse(ResponseCodes.RESPONSE_NORMAL, "Rdf commit and target stored", "text/plain");
		MetadataUtil.copyStoredIdentifier(putRdfBlobRes, res, null);
		return res;
	}
	
	protected Response putDataNonRdfCommit( RepoConfig repoConfig, Request req ) {
		Commit c = (Commit)req.getContent();
		Object target = c.getTarget();
		if( target instanceof Ref ) target = TheGetter.get( ((Ref)target).getTargetUri() );
		BaseRequest targetPutReq = new BaseRequest();
		targetPutReq.metadata = req.getMetadata();
		targetPutReq.content = target;
		Response targetPutRes = putData( repoConfig, targetPutReq );
		String targetUri = (String)targetPutRes.getContentMetadata().get(CcouchNamespace.RES_STORED_IDENTIFIER);
		if( targetUri == null ) throw new RuntimeException("Inserting entry target returned null");

		RdfCommit rdfDir = new RdfCommit(c, new BaseRef(targetUri));
		
		BaseRequest putRdfCommitReq = new BaseRequest();
		putRdfCommitReq.content = rdfDir;
		putRdfCommitReq.metadata = req.getMetadata();
		return putDataRdf( repoConfig, putRdfCommitReq );
	}

	protected Response putData( RepoConfig repoConfig, Request req ) {
		Object content = req.getContent();
		if( content == null ) {
			throw new RuntimeException("Can't put (null) to repo data store");
		} else if( content instanceof Directory ) {
			if( content instanceof RdfNode ) {
				return putDataRdfDirectory( repoConfig, req );
			} else {
				return putDataNonRdfDirectory( repoConfig, req );
			}
		} else if( content instanceof Commit ) {
			if( content instanceof RdfNode ) {
				return putDataRdfCommit( repoConfig, req );
			} else {
				return putDataNonRdfCommit( repoConfig, req );
			}
		} else if( content instanceof Blob ) {
			return putDataBlob( repoConfig, req );
		} else if( content instanceof Collection ) {
			BaseRequest subReq = new BaseRequest();
			subReq.content = ((Collection)content).iterator();
			subReq.metadata = req.getMetadata();
			return putData( repoConfig, subReq );
		} else if( content instanceof Iterator ) {
			int count = 0;
			for( Iterator i=(Iterator)content; i.hasNext(); ) {
				BaseRequest subReq = new BaseRequest();
				subReq.content = i.next();
				subReq.metadata = req.getMetadata();
				putData( repoConfig, subReq );
				++count;
			}
			return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, count + " items inserted", "text/plain");
		} else if( content instanceof Directory.Entry ) {
			BaseRequest subReq = new BaseRequest();
			subReq.metadata = req.getMetadata();
			subReq.content = ((Directory.Entry)content).getTarget();
			return putData( repoConfig, subReq );
		} else {
			throw new RuntimeException("Can't put " + content.getClass().getName() + " to repo data store");
		}
	}
	
	//// old put methods ////
	
	/*
	protected Response put( RepoConfig repoConfig, String path, Object value, Map valueMetadata, Map requestMetadata ) {
		BaseRequest subReq = new BaseRequest();
		subReq.uri = repoConfig.uri + "/" + path;
		subReq.verb = Request.VERB_PUT;
		subReq.metadata = requestMetadata;
		subReq.content = value;
		subReq.contentMetadata = valueMetadata;
		return put( subReq, repoConfig, path );
	}

	protected Response put( RepoConfig repoConfig, String path, Object value, Map requestMetadata ) {
		return put( repoConfig, path, value, Collections.EMPTY_MAP, requestMetadata );
	}
	
	protected Object getStoredObject( Response res ) {
		Object stored = res.getMetadata().get(CcouchNamespace.RES_STORED_OBJECT);
		if( stored != null ) return stored;
		String id = (String)res.getMetadata().get(CcouchNamespace.RES_STORED_IDENTIFIER);
		if( id != null ) return get(id);
		return null;
	}
	
	protected Response putHead( RepoConfig repoConfig, String path, Object content, Map contentMetadata, Map requestMetadata ) {
		HashMap subReqMetadata = new HashMap(requestMetadata);
		subReqMetadata.put(CcouchNamespace.REQ_FILEMERGE_METHOD, CcouchNamespace.REQ_FILEMERGE_STRICTIG);
		subReqMetadata.put(CcouchNamespace.REQ_STORE_SIMPLE_DIRS, Boolean.TRUE);
		Response dataRes = put( repoConfig, "data", content, contentMetadata, subReqMetadata );
		Object storedObject = getStoredObject(dataRes);
		
		String resolvedHeadPath = resolveHeadPath( repoConfig, path, storedObject instanceof Directory ? "" : "new" );
		BaseRequest subReq = new BaseRequest( Request.VERB_PUT, resolvedHeadPath );
		subReq.content = storedObject;
		subReq.contentMetadata = contentMetadata;
		subReq.metadata = new HashMap(requestMetadata);
		subReq.putMetadata(CcouchNamespace.REQ_DIRMERGE_METHOD, requestMetadata.get(CcouchNamespace.REQ_DIRMERGE_METHOD));
		subReq.putMetadata(CcouchNamespace.REQ_FILEMERGE_METHOD, requestMetadata.get(CcouchNamespace.REQ_FILEMERGE_METHOD));
		BaseResponse res = new BaseResponse(TheGetter.handleRequest(subReq));
		res.putMetadata(CcouchNamespace.RES_STORED_IDENTIFIER, dataRes.getMetadata().get(CcouchNamespace.RES_STORED_IDENTIFIER));
		return res;
	}
	
	protected Response put( final Request req, final RepoConfig repoConfig, String path ) {
		Object content = req.getContent();
		Blob blob;
		if( content instanceof Directory.Entry ) {
			return put( repoConfig, path, ((Directory.Entry)content).getTarget(), req.getMetadata() );
		} else if( content instanceof Iterator ) {
			int count = 0;
			for( Iterator i=(Iterator)content; i.hasNext(); ) {
				put( repoConfig, path, i.next(), req.getMetadata() );
				++count;
			}
			return new BaseResponse(Response.STATUS_NORMAL, count + " items inserted at " + repoConfig.uri + path);
		} else if( content instanceof Collection ) {
			return put( repoConfig, path, ((Collection)content).iterator(), req.getMetadata() );
		} else if( content instanceof Commit ) {
			// TODO: I'm starting to think that relying on 'rdfifier' to store stuff is the wrong approach.
			// It's confusing, especially when the thing passed in is *already* RDF.
			RdfNode storedRdf = new RdfCommit((Commit)content, getStoringTargetRdfifier(true, req, repoConfig));
			BaseRequest putRdfReq = new BaseRequest();
			putRdfReq.metadata = req.getMetadata();
			if( content instanceof RdfNode ) {
				putRdfReq.contentMetadata = req.getContentMetadata();
				putRdfReq.content = content;
			} else {
				putRdfReq.content = storedRdf;
			}
			return putDataRdf( repoConfig, putRdfReq );
		} else if( content instanceof Directory ) {
			// TODO: Re: rdfifying - see commit note, above 
			if( path.startsWith("data") ) {
				if( Boolean.TRUE.equals(req.getMetadata().get(CcouchNamespace.REQ_STORE_SIMPLE_DIRS)) ) {
					Directory storedDirectory = new SimpleDirectory( (Directory)content, new Function1() {
						public Object apply(Object input) {
							if( input instanceof Ref ) {
								input = TheGetter.get(((Ref)input).getTargetUri());
							}
							Response res = put(repoConfig, "data", input, req.getMetadata());
							return getStoredObject(res);
						}
					}, RdfDirectory.DEFAULT_TARGET_RDFIFIER);
					BaseResponse res = new BaseResponse(Response.STATUS_NORMAL, "Directory leaf blobs inserted to data", "text/plain");
					res.putMetadata(CcouchNamespace.RES_STORED_OBJECT, storedDirectory);
					return res;
				} else {
					RdfNode storedRdf = new RdfDirectory((Directory)content, getStoringTargetRdfifier(true, req, repoConfig));
					BaseRequest putRdfReq = new BaseRequest();
					putRdfReq.metadata = req.getMetadata();
					putRdfReq.content = storedRdf;
					return putDataRdf( repoConfig, putRdfReq );
				}
			} else if( path.startsWith("heads/") ) {
				return putHead( repoConfig, path, content, req.getContentMetadata(), req.getMetadata() );
			} else {
				throw new RuntimeException( "Can't PUT to " + req.getUri() + ": unrecognised post-repo path" );
			}
		} else if( content instanceof RdfNode ) {
			BaseRequest putRdfReq = new BaseRequest();
			putRdfReq.metadata = req.getMetadata();
			putRdfReq.content = content;
			return putDataRdf( repoConfig, putRdfReq );
		} else if( (blob = BlobUtil.getBlob(content, false)) != null ) {
			if( path.startsWith("data") ) {
				// subPath can be
				// data - post data to user store sector
				// data/<sector> - post data to named sector
				String sector = getRequestedStoreSector(req, repoConfig);
				
				byte[] hash = repoConfig.dataScheme.getHash(blob);
				String psp = hashToPostSectorPath(repoConfig, hash);
				String uri = repoConfig.uri + "data/" + sector + "/" + psp; 
				
				BaseRequest subReq = new BaseRequest( req, uri );
				subReq.content = blob;
				BaseResponse res = new BaseResponse( TheGetter.handleRequest(subReq) );
				res.putMetadata(CcouchNamespace.RES_STORED_IDENTIFIER, repoConfig.dataScheme.hashToUrn(hash));
				return res;
			} else if( path.startsWith("heads/") ) {
				return putHead( repoConfig, path, content, req.getContentMetadata(), req.getMetadata() );
			} else {
				throw new RuntimeException( "Can't PUT to " + req.getUri() + ": unrecognised post-repo path" );
			}
		} else if( content == null ) {
			throw new RuntimeException( "PUT/POST to " + req.getUri() + " requires content, null given.");
		} else {
			throw new RuntimeException( "Don't know how to PUT/POST " + content.getClass().getName() );
		}
	}
	*/
	
	//// Identify stuff ////
	
	protected String identifyBlob( Blob blob, RepoConfig repoConfig ) {
		FileHashCache fac = getFileHashCache(repoConfig.identificationScheme);
		if( fac != null && blob instanceof FileBlob ) {
			return fac.getUrn((FileBlob)blob);
		}
		Log.log(Log.EVENT_PERFORMANCE_WARNING, "Unable to cache URN of "+blob.getClass().getName());
		return repoConfig.identificationScheme.hashToUrn(repoConfig.identificationScheme.getHash(blob));
	}
	
	protected Response identify( RepoConfig repoConfig, Object content, Map contentMetadata ) {
		String id = null;
		if( content instanceof RdfNode ) {
			Object parsedFrom = contentMetadata.get(CcouchNamespace.PARSED_FROM);
			if( parsedFrom == null ) {
				Log.log(Log.EVENT_PERFORMANCE_WARNING, "Blobbifying RDF node just to get content URN");
				parsedFrom = BlobUtil.getBlob( content.toString() );
			}
			String parsedFromUri = ValueUtil.getString(identify( repoConfig, parsedFrom, Collections.EMPTY_MAP ).getContent());
			// TODO: Make sure repoConfig.dataScheme.wouldHandleUrn(parsedFromUri)?
			return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, "x-rdf-subject:" + parsedFromUri, "text/plain");
		} else if( content instanceof Commit ) {
			return identify( repoConfig, new RdfCommit( (Commit)content, getTargetRdfifier(false, false) ), contentMetadata );
		} else if( content instanceof Directory ) {
			return identify( repoConfig, new RdfDirectory( (Directory)content, getTargetRdfifier(false, false) ), contentMetadata );
		} else if( content instanceof Ref && repoConfig.identificationScheme.couldTranslateUrn(((Ref)content).getTargetUri()) ) {
			id = ((Ref)content).getTargetUri();
		} else {
			content = TheGetter.dereference(content);
			Blob blob = BlobUtil.getBlob(content, false);
			if( blob != null ) id = identifyBlob( blob, repoConfig );
		}
		
		if( id != null ) return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, id, "text/plain");
		
		throw new RuntimeException("I don't know how to identify " + (content == null ? "null" : content.getClass().getName()));
	}
	
	////
	
	protected static String FUNCTION_RESULT_CACHE_PREFIX = "function-result-cache/";
	Map slfFiles = new HashMap();
	
	protected synchronized SimpleListFile getSlf( File f ) {
		SimpleListFile slf = (SimpleListFile)slfFiles.get( f.getAbsolutePath() );
		if( slf == null ) {
			try {
				FileUtil.mkParentDirs(f);
				slf = new SimpleListFile(f, "rw");
				slf.init(65536, 1024*1024);
				slfFiles.put(f.getAbsolutePath(), slf);
			} catch( IOException e ) {
				throw new RuntimeException("Couldn't open cache file in 'w' mode", e);
			}
		}
		return slf;
	}
	
	protected synchronized SimpleListFile getSlf( RepoConfig repoConfig, String subPath ) {
		PathUtil.Path p = PathUtil.parseFilePathOrUri( repoConfig.uri );
		return getSlf( new File(p.toString() + subPath) );
	}
	
	protected void putFunctionResult( RepoConfig repoConfig, String subIndexName, String key, String value ) {
		SimpleListFile slf = getSlf( repoConfig, "cache/function-results/"+subIndexName+".slf" );
		try {
			slf.put(key, ValueUtil.getBytes("\""+value) );
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	protected void putFunctionResultUri( RepoConfig repoConfig, String subIndexName, String key, String valueUri ) {
		SimpleListFile slf = getSlf( repoConfig, "cache/function-results/"+subIndexName+".slf" );
		try {
			slf.put(key, ValueUtil.getBytes("<"+valueUri) );
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	protected void putFunctionResult( RepoConfig repoConfig, String subIndexName, String key, Object value ) {
		if( value instanceof Ref ) {
			putFunctionResultUri( repoConfig, subIndexName, key, ((Ref)value).getTargetUri() );
		} else {
			putFunctionResult( repoConfig, subIndexName, key, ValueUtil.getString(value) );
		}
	}
	
	protected Object getFunctionResult( RepoConfig repoConfig, String subIndexName, String key ) {
		SimpleListFile slf = getSlf( repoConfig, "cache/function-results/"+subIndexName+".slf" );
		try {
			byte[] b = slf.get(key);
			if( b == null ) {
				return null;
			} else if( b[0] == (byte)'<' ) {
				return new BaseRef(ValueUtil.getString(b, 1, b.length-1));
			} else {
				return ValueUtil.getString(b, 1, b.length-1);
			}
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
		/*
		LuceneIndex li = openLuceneIndex( repoConfig, "function-result-cache/"+subIndexName, true, false );
		if( li == null ) return null;
		Document doc = li.getPairDocument("key", key);
		if( doc == null ) return null;
		String value = doc.get("value");
		String valueUri = doc.get("valueUri");
		if( valueUri != null ) return new BaseRef(valueUri);
		return value;
		*/
	}

	////
	
	MetaRepoConfig config;
	
	public MetaRepository( MetaRepoConfig config ) {
		this.config = config;
	}
	
	protected RepoConfig lastHitRepoConfig;
	protected String lastHitDataSectorUri;
	
	//// Validate and cache blobs fetched from remote repos ////
	
	protected static class HashMismatchException extends Exception {
		public ContentAddressingScheme dataScheme;
		public byte[] expected, calculated;
		public HashMismatchException( ContentAddressingScheme dataScheme, byte[] expected, byte[] calculated ) {
			this.dataScheme = dataScheme;
			this.expected = expected;
			this.calculated = calculated;
		}
	}
	
	protected Response maybeCacheBlob( Response res, Request req ) throws HashMismatchException {
		String urn = req.getResourceName();
		String cs = (String)req.getMetadata().get(CcouchNamespace.REQ_CACHE_SECTOR);
		if( cs != null ) {
			if( !config.defaultRepoConfig.storageScheme.couldTranslateUrn(urn) ) {
				throw new RuntimeException("Don't know how to translate "+urn+" for storage" );
			}
			
			// Then fetch the blob, validate it, store it, fetch what you just stored back out of
			// the store, and give *that* to the user.
			Blob blob = BlobUtil.getBlob(res.getContent(), true);
			boolean verified = false;
			for( Iterator i=Schemes.allSchemes.iterator(); !verified && i.hasNext(); ) {
				ContentAddressingScheme cas = (ContentAddressingScheme)i.next();
				if( !cas.verify(urn, blob) ) {
					throw new HashMismatchException( cas, cas.urnToHash(urn), cas.getHash(blob) );
				}
				verified = true;
			}
			if( !verified ) {
				throw new RuntimeException("No ContentAddressingScheme available to verify "+urn);
			}
			
			// Otherwise it's all good...we'll store the blob
			BaseRequest putReq = new BaseRequest(req);
			putReq.verb = RequestVerbs.VERB_PUT;
			putReq.uri = "data"; // Will not be used...
			putReq.content = res.getContent();
			putReq.contentMetadata = res.getContentMetadata();
			putReq.metadata = req.getMetadata();
			putReq.putMetadata(CcouchNamespace.REQ_STORE_SECTOR, cs);
			Response putRes = putData(config.defaultRepoConfig, putReq);
			
			String storedUrn = MetadataUtil.getStoredIdentifier( putRes );
			if( storedUrn == null ) {
				throw new RuntimeException( "Received no stored URN when caching" );
			}
			
			if( !storedUrn.equals(urn)) {
				Log.log( Log.EVENT_WARNING, "After caching "+urn+", stored blob has different URN: "+storedUrn );
			}
			
			Log.log(Log.EVENT_STORED, "Cached "+urn+" oh yeah");
			
			// Then fetch it again.
			return call(new BaseRequest( req, storedUrn ));
		}
		return res;
	}
		
	protected void logHashMismatch( Request req, ContentAddressingScheme dataScheme, byte[] expected, byte[] calculated ) {
		Log.log(Log.EVENT_WARNING, "Received apparently bad content from "+req.getResourceName()+"; expected hash="+
			dataScheme.hashToUrn(expected) + ", received="+dataScheme.hashToUrn(calculated)
		);
	}
	
	//// Handle requests ////
		
	public Response call( Request req ) {
		if( "x-ccouch-repo://".equals(req.getResourceName()) ) {
			// Return a directory listing of all registered repositories
			SimpleDirectory sd = new SimpleDirectory();
			for( Iterator i=this.config.namedRepoConfigs.values().iterator(); i.hasNext(); ) {
				RepoConfig repoConfig = (RepoConfig)i.next();
				SimpleDirectory.Entry entry = new SimpleDirectory.Entry();
				entry.name = repoConfig.name;
				entry.targetType = CcouchNamespace.TT_SHORTHAND_DIRECTORY;
				entry.target = new BaseRef("x-ccouch-repo:all-repos-dir", entry.name + "/", repoConfig.uri);
				sd.addDirectoryEntry(entry, Collections.EMPTY_MAP);
			}
			return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, sd);
		} else if( req.getResourceName().startsWith("x-ccouch-head:") || req.getResourceName().startsWith("x-ccouch-repo:") ) {
			req = MfArgUtil.argumentizeQueryString(req);
			RepoRef repoRef = RepoRef.parse(req.getResourceName(), false);
			RepoConfig repoConfig;
			if( repoRef.repoName == null ) {
				repoConfig = config.defaultRepoConfig;
				if( repoConfig == null ) {
					BaseResponse res = new BaseResponse(ResponseCodes.RESPONSE_DOESNOTEXIST, "No default repository to handle " + req.getResourceName());
					res.putContentMetadata(DcNamespace.DC_FORMAT, "text/plain");
					return res;
				}
			} else {
				repoConfig = (RepoConfig)config.namedRepoConfigs.get(repoRef.repoName);
				if( repoConfig == null ) {
					BaseResponse res = new BaseResponse(ResponseCodes.RESPONSE_DOESNOTEXIST, "No such repository: " + repoRef.repoName);
					res.putContentMetadata(DcNamespace.DC_FORMAT, "text/plain");
					return res;
				}
			}
			
			if( "identify".equals(repoRef.subPath) ) {
				return identify( repoConfig, MfArgUtil.getPrimaryArgument(req.getContent()), req.getContentMetadata() );
			}
			
			if( RequestVerbs.VERB_PUT.equals(req.getVerb()) || RequestVerbs.VERB_POST.equals(req.getVerb()) ) {
				if( repoRef.subPath.startsWith(FUNCTION_RESULT_CACHE_PREFIX) ) {
					String[] pathParts = repoRef.subPath.substring(FUNCTION_RESULT_CACHE_PREFIX.length()).split("/");
					String subIndexName, key;
					if( pathParts.length > 1 && pathParts[0].length() > 0 ) {
						subIndexName = pathParts[0];
						key = UriUtil.uriDecode(pathParts[1]);
					} else {
						subIndexName = getRequestedStoreSector( req, repoConfig );
						key = UriUtil.uriDecode(pathParts[0]);
					}
					
					putFunctionResult( repoConfig, subIndexName, key, req.getContent() );
					return new BaseResponse( ResponseCodes.RESPONSE_NORMAL, "OK" );
				} else if( repoRef.subPath.equals("data") || repoRef.subPath.startsWith("data/") ) {
					String[] dataAndSector = repoRef.subPath.split("/");
					if( dataAndSector.length > 1 && dataAndSector[1].length() > 0 ) {
						BaseRequest sectorOverrideReq = new BaseRequest(req);
						sectorOverrideReq.metadata.put(CcouchNamespace.REQ_STORE_SECTOR, dataAndSector[2]);
						req = sectorOverrideReq;
					}
					
					return putData( repoConfig, req );
				} else if( repoRef.subPath.startsWith("heads/") ) {
					String headPath = repoRef.subPath.substring("heads/".length());
					BaseRequest subReq = new BaseRequest( req, resolveHeadPath(repoConfig, headPath, "") );
					return TheGetter.call(subReq);
				} else {
					throw new RuntimeException("Can't PUT or POST to " + req.getResourceName() + ", sorry!");
				}
			} else {
				String path = repoRef.subPath;
				if( path.startsWith("heads/") ) {
					String headPath = repoRef.subPath.substring("heads/".length());
					path = resolveHeadPath(repoConfig, headPath, "");
				} else if( path.startsWith("parsed-heads/") ) {
					String inputHeadPath = repoRef.subPath.substring("parsed-heads/".length());
					String headPath = inputHeadPath;
					String resolvedHeadPath = resolveHeadPath(repoConfig, headPath, "");

					// Try just getting it
					BaseRequest subReq = new BaseRequest(req, resolvedHeadPath);
					Response subRes = TheGetter.call(subReq);
					if( subRes.getStatus() == ResponseCodes.RESPONSE_NORMAL ) {
						return subRes;
					}
					
					// If that fails, try getting initial parts before '/'
					// and parsing them
					while( headPath.lastIndexOf('/') != -1 ) {
						headPath = headPath.substring(0, headPath.lastIndexOf('/'));
						resolvedHeadPath = resolveHeadPath(repoConfig, headPath, "");
						subReq = new BaseRequest(req, resolvedHeadPath);
						subRes = TheGetter.call(subReq);
						if( subRes.getStatus() == ResponseCodes.RESPONSE_NORMAL ) {
							String headContent = ValueUtil.getString( subRes.getContent() );
							Object head = RdfIO.parseRdf(headContent, resolvedHeadPath);
							return FollowPath.followPath( req, head, inputHeadPath.substring(headPath.length()+1));
						}
					}
					
					return subRes;
				} else if( path.equals("files") ) {
					path = repoConfig.uri + "files";
				} else if( path.startsWith("files/") ) {
					path = repoConfig.uri + path.substring("files/".length());
				} else if( path.startsWith(FUNCTION_RESULT_CACHE_PREFIX) ) {
					String[] pathParts = repoRef.subPath.substring(FUNCTION_RESULT_CACHE_PREFIX.length()).split("/");
					String subIndexName, key;
					if( pathParts.length > 1 && pathParts[0].length() > 0 ) {
						subIndexName = pathParts[0];
						key = UriUtil.uriDecode(pathParts[1]);
					} else {
						subIndexName = getRequestedStoreSector( req, repoConfig );
						key = UriUtil.uriDecode(pathParts[0]);
					}
					
					return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, getFunctionResult(repoConfig, subIndexName, key));
				} else if( path.equals("") ) {
					SimpleDirectory sd = new SimpleDirectory();
					sd.addDirectoryEntry(new SimpleDirectory.Entry("files",new BaseRef(req.getResourceName(),"files"),CcouchNamespace.TT_SHORTHAND_DIRECTORY));
					sd.addDirectoryEntry(new SimpleDirectory.Entry("heads",new BaseRef(req.getResourceName(),"heads"),CcouchNamespace.TT_SHORTHAND_DIRECTORY));
					sd.addDirectoryEntry(new SimpleDirectory.Entry("parsed-heads",new BaseRef(req.getResourceName(),"parsed-heads"),CcouchNamespace.TT_SHORTHAND_DIRECTORY));
					return new BaseResponse( 200, sd );
				}
				BaseRequest subReq = new BaseRequest(req, path);
				return TheGetter.call(subReq);
			}
			
			//String sector = MetadataUtil.getKeyed(request.getMetadata(), RdfNamespace.STORE_SECTOR, rc.userStoreSector);
		} else {
			if( RequestVerbs.VERB_GET.equals(req.getVerb()) || RequestVerbs.VERB_HEAD.equals(req.getVerb()) ) {
				// URN request? Check each repo to see if it has a data scheme that would handle it

				String urn = req.getResourceName();
			
				// Check local repos
				for( Iterator i=config.getDefaultAndLocalRepoConfigs().iterator(); i.hasNext(); ) {
					RepoConfig repoConfig = (RepoConfig)i.next();
					String psp = urnToPostSectorPath(repoConfig, urn);
					if( psp == null ) continue;
	
					List dataSectorUris = getRepoDataSectorUrls(repoConfig);
					for( Iterator si=dataSectorUris.iterator(); si.hasNext(); ) {
						String dataSectorUri = (String)si.next();
						BaseRequest subReq = new BaseRequest(req, PathUtil.appendPath(dataSectorUri, psp));
						Response res = TheGetter.call(subReq);
						if( res.getStatus() == ResponseCodes.RESPONSE_NORMAL ) return res;
					}
				}
				
				// Check most recently hit remote repo
				lastHit: if( lastHitDataSectorUri != null ) {
					String psp = urnToPostSectorPath(lastHitRepoConfig, urn);
					if( psp == null ) break lastHit;
					
					BaseRequest subReq = new BaseRequest(req, PathUtil.appendPath(lastHitDataSectorUri, psp));
					Response res = TheGetter.call(subReq);
					if( res.getStatus() == ResponseCodes.RESPONSE_NORMAL ) {
						try {
							return maybeCacheBlob(res, req);
						} catch( HashMismatchException hme ) {
							logHashMismatch( subReq, hme.dataScheme, hme.expected, hme.calculated );
						}
					}
				}
				
				// Check all remote repos (unless we are explicitly asked not to)
				if( !MetadataUtil.isEntryTrue(req.getMetadata(), CcouchNamespace.REQ_LOCAL_REPOS_ONLY))
				for( Iterator i=config.remoteRepoConfigs.iterator(); i.hasNext(); ) {
					RepoConfig repoConfig = (RepoConfig)i.next();
					String psp = urnToPostSectorPath(repoConfig, urn);
					if( psp == null ) continue;
	
					List dataSectorUris = getRepoDataSectorUrls(repoConfig);
					for( Iterator si=dataSectorUris.iterator(); si.hasNext(); ) {
						String dataSectorUri = (String)si.next();
						BaseRequest subReq = new BaseRequest(req, PathUtil.appendPath(dataSectorUri, psp));
						Response res = TheGetter.call(subReq);
						if( res.getStatus() == ResponseCodes.RESPONSE_NORMAL ) {
							lastHitDataSectorUri = dataSectorUri;
							lastHitRepoConfig = repoConfig;
							try {
								return maybeCacheBlob(res, req);
							} catch( HashMismatchException hme ) {
								logHashMismatch( subReq, hme.dataScheme, hme.expected, hme.calculated );
							}
						}
					}
				}
			}
		}
		return BaseResponse.RESPONSE_UNHANDLED;
	}
	
	public Function1 getTargetRdfifier(final boolean nested, final boolean followRefs) {
		return new Function1() {
			public Object apply(Object input) {
				if( followRefs && input instanceof Ref ) {
					input = TheGetter.get(((Ref)input).getTargetUri());
				}
				
				if( input instanceof Directory ) {
					RdfDirectory rdfDir = new RdfDirectory((Directory)input, this); 
					if( nested ) {
						return rdfDir;
					} else {
						return new BaseRef("x-rdf-subject:" + identifyBlob(BlobUtil.getBlob(rdfDir.toString()), config.defaultRepoConfig));
					}
				} else if( input instanceof Ref ) {
					return input;
				} else if( input instanceof Blob ) {
					return new BaseRef(identifyBlob((Blob)input, config.defaultRepoConfig));
				} else {
					throw new RuntimeException("Don't know how to rdf-ify " + input.getClass().getName() );
				}
			}
		};
	}
}
