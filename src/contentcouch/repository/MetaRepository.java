package contentcouch.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import togos.rra.BaseRequest;
import togos.rra.BaseRequestHandler;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;

import com.eekboom.utils.Strings;

import contentcouch.blob.BlobUtil;
import contentcouch.directory.WritableDirectory;
import contentcouch.file.FileBlob;
import contentcouch.hashcache.FileHashCache;
import contentcouch.misc.Function1;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.SimpleDirectory;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.rdf.DcNamespace;
import contentcouch.rdf.RdfCommit;
import contentcouch.rdf.RdfDirectory;
import contentcouch.rdf.RdfNode;
import contentcouch.store.TheGetter;
import contentcouch.value.BaseRef;
import contentcouch.value.Blob;
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
	
	protected FileHashCache fileHashCacheCache;
	protected FileHashCache getFileHashCache() {
		if( fileHashCacheCache == null && config.defaultRepoConfig.uri.startsWith("file:") ) {
			String hashCachePath = PathUtil.parseFilePathOrUri(config.defaultRepoConfig.uri + "cache/file-attrs.slf").toString();
			File cacheFile = new File(hashCachePath);
			fileHashCacheCache = new FileHashCache(cacheFile);
		}
		return fileHashCacheCache;
	}
	
	public byte[] getHash( RepoConfig repoConfig, Blob blob ) {
		if( blob instanceof FileBlob ) {
			FileHashCache fileHashCache = getFileHashCache();
			return fileHashCache.getHash((FileBlob)blob, repoConfig.dataScheme);
		}
		return repoConfig.dataScheme.getHash( blob );
	}
	
	protected String hashToPostSectorPath( RepoConfig repoConfig, byte[] hash ) {
		String filename = repoConfig.dataScheme.hashToFilename(hash);
		return filenameToPostSectorPath(repoConfig, filename);
	}
	
	protected String urnToPostSectorPath( RepoConfig repoConfig, String urn ) {
		if( !repoConfig.dataScheme.wouldHandleUrn(urn) ) return null;
		byte[] hash = repoConfig.dataScheme.urnToHash(urn);
		return hashToPostSectorPath( repoConfig, hash );
	}
	
	protected List getRepoDataSectorUrls( RepoConfig repoConfig ) {
		ArrayList l = new ArrayList();
		String dataDirUri = PathUtil.appendPath(repoConfig.uri,"data/");
		
		BaseRequest dirReq = new BaseRequest( Request.VERB_GET, dataDirUri );
		Response dirRes = TheGetter.handleRequest(dirReq);
		if( dirRes.getStatus() != Response.STATUS_NORMAL ) return l;
		if( !(dirRes.getContent() instanceof Directory) ) {
			System.err.println(dataDirUri + " exists but is not a directory");
		}
		Directory d = (Directory)dirRes.getContent();
		for( Iterator i=d.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry e = (Directory.Entry)i.next();
			if( CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(e.getTargetType()) ) {
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
	
	protected String resolveHeadPath( RepoConfig repoConfig, String path, String defaultBaseName ) {
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
		
		byte[] hash = getHash(repoConfig, (Blob)req.getContent());
		
		String psp = hashToPostSectorPath(repoConfig, hash);
		String uri = repoConfig.uri + "data/" + sector + "/" + psp; 
		
		BaseRequest subReq = new BaseRequest( req, uri );
		subReq.verb = Request.VERB_PUT;
		BaseResponse res = new BaseResponse( TheGetter.handleRequest(subReq) );
		res.putMetadata(CcouchNamespace.RES_STORED_IDENTIFIER, repoConfig.dataScheme.hashToUrn(hash));
		Date mtime = (Date)req.getContentMetadata().get(DcNamespace.DC_MODIFIED);
		if( mtime != null ) {
			res.putMetadata(CcouchNamespace.RES_HIGHEST_BLOB_MTIME, mtime);
		}
		return res;
	}

	protected Response putDataRdf( RepoConfig repoConfig, Request req ) {
		Object parsedFrom = req.getContentMetadata().get(CcouchNamespace.PARSED_FROM);
		BaseRequest subReq = new BaseRequest();
		subReq.metadata = req.getMetadata();
		subReq.content = parsedFrom != null ? parsedFrom : BlobUtil.getBlob(((RdfNode)req.getContent()).toString());
		Response blobPutRes = putDataBlob( repoConfig, subReq );
		if( blobPutRes.getStatus() != Response.STATUS_NORMAL ) return blobPutRes;
		BaseResponse res = new BaseResponse();
		MetadataUtil.copyStoredIdentifier(blobPutRes, res, "x-parse-rdf:");
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
			BaseResponse res = new BaseResponse(Response.STATUS_NORMAL, "Rdf directory already stored - skipping", "text/plain");
			res.putMetadata(CcouchNamespace.RES_DEST_ALREADY_EXISTED, Boolean.TRUE);
			MetadataUtil.copyStoredIdentifier(putRdfBlobRes, res, null);
			return res;
		}

		Directory d = (Directory)req.getContent();
		for( Iterator i=d.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry e = (Directory.Entry)i.next();
			Object target = e.getTarget();
			String targetSourceUri;
			if( target instanceof Ref ) {
				targetSourceUri = ((Ref)target).getTargetUri();
				target = TheGetter.get( targetSourceUri );
			} else {
				String sourceUri = (String)req.getContentMetadata().get(CcouchNamespace.SOURCE_URI);
				if( sourceUri != null ) {
					targetSourceUri = PathUtil.appendPath(sourceUri, e.getName());
				} else {
					targetSourceUri = null;
				}
			}
			BaseRequest targetPutReq = new BaseRequest();
			targetPutReq.content = target;
			targetPutReq.metadata = req.getMetadata();
			targetPutReq.putContentMetadata(CcouchNamespace.SOURCE_URI, targetSourceUri);
			putData( repoConfig, targetPutReq );
		}
		
		BaseResponse res = new BaseResponse(Response.STATUS_NORMAL, "Rdf directory and entries stored", "text/plain");
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
				// TODO: Some kind of validation on the URI (should be x-parse-rdf:<urn scheme>:...)
				BaseResponse res = new BaseResponse(Response.STATUS_NORMAL, "URI previously cached in .ccouch-uri file - skipping", "text/plain");
				res.putMetadata(CcouchNamespace.RES_STORED_IDENTIFIER, uri);
				return res;
			}
		}		
		
		//(Date)req.getContentMetadata().get(DcNamespace.DC_MODIFIED);
		Date highestMtime = null; // Don't pay attention to directory mtime
		List rdfDirectoryEntries = new ArrayList();
		for( Iterator i=d.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry e = (Directory.Entry)i.next();
			Object target = e.getTarget();
			String targetSourceUri;
			if( target instanceof Ref ) {
				targetSourceUri = ((Ref)target).getTargetUri();
				target = TheGetter.get( targetSourceUri );
			} else {
				String sourceUri = (String)req.getContentMetadata().get(CcouchNamespace.SOURCE_URI);
				if( sourceUri != null ) {
					targetSourceUri = PathUtil.appendPath(sourceUri, e.getName());
				} else {
					targetSourceUri = null;
				}
			}
			BaseRequest targetPutReq = new BaseRequest();
			targetPutReq.content = target;
			targetPutReq.metadata = req.getMetadata();
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
			BaseResponse res = new BaseResponse(Response.STATUS_NORMAL, "Rdf commit already stored - skipping", "text/plain");
			res.putMetadata(CcouchNamespace.RES_DEST_ALREADY_EXISTED, Boolean.TRUE);
			MetadataUtil.copyStoredIdentifier(putRdfBlobRes, res, null);
			return res;
		}

		Commit c = (Commit)req.getContent();
		Object target = c.getTarget();
		String targetSourceUri;
		if( target instanceof Ref ) {
			targetSourceUri = ((Ref)target).getTargetUri();
			target = TheGetter.get( targetSourceUri );
		} else {
			targetSourceUri = null;
		}
		BaseRequest targetPutReq = new BaseRequest();
		targetPutReq.metadata = req.getMetadata();
		targetPutReq.content = target;
		targetPutReq.putContentMetadata(CcouchNamespace.SOURCE_URI, targetSourceUri);
		putData( repoConfig, targetPutReq );
		
		BaseResponse res = new BaseResponse(Response.STATUS_NORMAL, "Rdf commit and target stored", "text/plain");
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
			return new BaseResponse(Response.STATUS_NORMAL, count + " items inserted", "text/plain");
		} else if( content instanceof Directory.Entry ) {
			BaseRequest subReq = new BaseRequest();
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
	
	protected String identifyBlob( Blob blob, RepoConfig repoConfig ) {
		return repoConfig.dataScheme.hashToUrn(getHash(repoConfig, blob));
	}
	
	protected Response identify( RepoConfig repoConfig, Object content, Map contentMetadata ) {
		if( content instanceof RdfNode ) {
			Object parsedFrom = contentMetadata.get(CcouchNamespace.PARSED_FROM);
			if( parsedFrom == null ) parsedFrom = BlobUtil.getBlob( content.toString() );
			String parsedFromUri = ValueUtil.getString(identify( repoConfig, parsedFrom, Collections.EMPTY_MAP ).getContent());
			return new BaseResponse(Response.STATUS_NORMAL,	"x-parse-rdf:" + parsedFromUri, "text/plain");
		} else if( content instanceof Commit ) {
			return identify( repoConfig, new RdfCommit( (Commit)content, getTargetRdfifier(false, false) ), contentMetadata );
		} else if( content instanceof Directory ) {
			return identify( repoConfig, new RdfDirectory( (Directory)content, getTargetRdfifier(false, false) ), contentMetadata );
		}

		Blob blob = BlobUtil.getBlob(content, false);
		if( blob != null ) return new BaseResponse(Response.STATUS_NORMAL, identifyBlob( blob, repoConfig ), "text/plain");
		
		throw new RuntimeException("I don't know how to identify " + content.getClass().getName());
	}
	
	//
	
	MetaRepoConfig config;
	
	public MetaRepository( MetaRepoConfig config ) {
		this.config = config;
	}
	
	protected RepoConfig lastHitRepoConfig;
	protected String lastHitDataSectorUri;
	
	public Response handleRequest( Request req ) {
		if( "x-ccouch-repo://".equals(req.getUri()) ) {
			SimpleDirectory sd = new SimpleDirectory();
			for( Iterator i=this.config.namedRepoConfigs.values().iterator(); i.hasNext(); ) {
				RepoConfig repoConfig = (RepoConfig)i.next();
				SimpleDirectory.Entry entry = new SimpleDirectory.Entry();
				entry.name = repoConfig.name;
				entry.targetType = CcouchNamespace.OBJECT_TYPE_DIRECTORY;
				entry.target = new BaseRef("x-ccouch-repo:all-repos-dir", entry.name + "/", repoConfig.uri);
				sd.addDirectoryEntry(entry);
			}
			return new BaseResponse(Response.STATUS_NORMAL, sd);
		} else if( req.getUri().startsWith("x-ccouch-head:") || req.getUri().startsWith("x-ccouch-repo:") ) {
			RepoRef repoRef = RepoRef.parse(req.getUri(), false);
			RepoConfig repoConfig;
			if( repoRef.repoName == null ) {
				repoConfig = config.defaultRepoConfig;
				if( repoConfig == null ) {
					BaseResponse res = new BaseResponse(Response.STATUS_DOESNOTEXIST, "No default repository to handle " + req.getUri());
					res.putContentMetadata(DcNamespace.DC_FORMAT, "text/plain");
					return res;
				}
			} else {
				repoConfig = (RepoConfig)config.namedRepoConfigs.get(repoRef.repoName);
				if( repoConfig == null ) {
					BaseResponse res = new BaseResponse(Response.STATUS_DOESNOTEXIST, "No such repository: " + repoRef.repoName);
					res.putContentMetadata(DcNamespace.DC_FORMAT, "text/plain");
					return res;
				}
			}
			
			if( Request.VERB_PUT.equals(req.getVerb()) || Request.VERB_POST.equals(req.getVerb()) ) {
				if( "identify".equals(repoRef.subPath) ) {
					return identify( repoConfig, req.getContent(), req.getContentMetadata() );
				} else if( repoRef.subPath.equals("data") || repoRef.subPath.startsWith("data/") ) {
					String[] dataAndSector = repoRef.subPath.split("/");
					if( dataAndSector.length > 1 ) {
						BaseRequest sectorOverrideReq = new BaseRequest();
						sectorOverrideReq.metadata = req.getMetadata();
						sectorOverrideReq.metadata.put(CcouchNamespace.REQ_STORE_SECTOR, dataAndSector[1]);
						sectorOverrideReq.content = req.getContent();
						sectorOverrideReq.contentMetadata = req.getContentMetadata();
						req = sectorOverrideReq;
					}
					
					return putData( repoConfig, req );
				} else if( repoRef.subPath.startsWith("heads/") ) {
					BaseRequest subReq = new BaseRequest( req, resolveHeadPath(repoConfig, repoRef.subPath, "") );
					return TheGetter.handleRequest(subReq);
				} else {
					throw new RuntimeException("Can't PUT to " + req.getUri() + ", sorry!");
				}
			} else {
				String path = repoRef.subPath;
				if( path.startsWith("heads/") ) {
					path = resolveHeadPath(repoConfig, path, "");
				} else {
					path = repoConfig.uri + path;
				}
				BaseRequest subReq = new BaseRequest(req, path);
				return TheGetter.handleRequest(subReq);
			}
			
			//String sector = MetadataUtil.getKeyed(request.getMetadata(), RdfNamespace.STORE_SECTOR, rc.userStoreSector);
		} else {
			if( Request.VERB_GET.equals(req.getVerb()) || Request.VERB_HEAD.equals(req.getVerb()) ) {
				// URN request? Check each repo to see if it has a data scheme that would handle it

				String urn = req.getUri();
			
				// Check local repos
				for( Iterator i=config.getDefaultAndLocalRepoConfigs().iterator(); i.hasNext(); ) {
					RepoConfig repoConfig = (RepoConfig)i.next();
					String psp = urnToPostSectorPath(repoConfig, urn);
					if( psp == null ) continue;
	
					List dataSectorUris = getRepoDataSectorUrls(repoConfig);
					for( Iterator si=dataSectorUris.iterator(); si.hasNext(); ) {
						String dataSectorUri = (String)si.next();
						BaseRequest subReq = new BaseRequest(req, PathUtil.appendPath(dataSectorUri, psp));
						Response res = TheGetter.handleRequest(subReq);
						if( res.getStatus() == Response.STATUS_NORMAL ) return res;
					}
				}
				
				// Check most recently hit remote repo
				lastHit: if( lastHitDataSectorUri != null ) {
					String psp = urnToPostSectorPath(lastHitRepoConfig, urn);
					if( psp == null ) break lastHit;
					
					BaseRequest subReq = new BaseRequest(req, PathUtil.appendPath(lastHitDataSectorUri, psp));
					Response res = TheGetter.handleRequest(subReq);
					if( res.getStatus() == Response.STATUS_NORMAL ) return res;
				}
				
				// Check all remote repos
				for( Iterator i=config.remoteRepoConfigs.iterator(); i.hasNext(); ) {
					RepoConfig repoConfig = (RepoConfig)i.next();
					String psp = urnToPostSectorPath(repoConfig, urn);
					if( psp == null ) continue;
	
					List dataSectorUris = getRepoDataSectorUrls(repoConfig);
					for( Iterator si=dataSectorUris.iterator(); si.hasNext(); ) {
						String dataSectorUri = (String)si.next();
						BaseRequest subReq = new BaseRequest(req, PathUtil.appendPath(dataSectorUri, psp));
						Response res = TheGetter.handleRequest(subReq);
						if( res.getStatus() == Response.STATUS_NORMAL ) {
							lastHitDataSectorUri = dataSectorUri;
							lastHitRepoConfig = repoConfig;
							return res;
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
						return new BaseRef("x-parse-rdf:" + identifyBlob(BlobUtil.getBlob(rdfDir.toString()), config.defaultRepoConfig));
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
