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

import com.eekboom.utils.Strings;

import contentcouch.activefunctions.FollowPath;
import contentcouch.app.Log;
import contentcouch.blob.Blob;
import contentcouch.blob.BlobUtil;
import contentcouch.contentaddressing.ContentAddressingScheme;
import contentcouch.contentaddressing.Schemes;
import contentcouch.context.Config;
import contentcouch.directory.SimpleDirectory;
import contentcouch.directory.WritableDirectory;
import contentcouch.file.FileBlob;
import contentcouch.file.FileUtil;
import contentcouch.framework.BaseRequestHandler;
import contentcouch.framework.MFArgUtil;
import contentcouch.framework.TheGetter;
import contentcouch.framework.err.AbnormalResponseException;
import contentcouch.framework.err.NotFoundException;
import contentcouch.hashcache.FileHashCache;
import contentcouch.hashcache.SimpleListFile;
import contentcouch.merge.MergeUtil;
import contentcouch.misc.Function1;
import contentcouch.misc.MapUtil;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.rdf.DcNamespace;
import contentcouch.rdf.RdfCommit;
import contentcouch.rdf.RdfDirectory;
import contentcouch.rdf.RdfIO;
import contentcouch.rdf.RdfNode;
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
	
	/** Use this to cache values instead of the SLF for unit tests. */
	public Map stringCacheOverride;
	
	protected String normalizeRdfString( String rdf ) {
		return rdf.trim()+"\n";
	}
	
	protected String filenameToPostSectorPath( RepoConfig repoConfig, String filename ) {
		if( filename.length() >= 2 ) {
			return filename.substring(0,2) + "/" + filename;
		} else {
			return filename;
		}
	}
	
	//// Hash cache stuff ////
	
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
	
	protected void maybeCacheDirectoryUrn( Directory dir, String urn, Map options ) {
		if( dir instanceof WritableDirectory &&
			MetadataUtil.isEntryTrue(options,CCouchNamespace.REQ_CREATE_URI_DOT_FILES) &&
			urn != null
		) {
			cacheDirectoryUrnInDotFile( (WritableDirectory)dir, urn );
		}
		
		if( dir instanceof File && MetadataUtil.isEntryTrue(options,CCouchNamespace.REQ_CACHE_DIRECTORY_HASHES) ) {
			cacheDirectoryUrnInDatabase( (File)dir, urn );
		}
	}
	
	protected void cacheDirectoryUrnInDotFile( WritableDirectory dir, String urn ) {
		SimpleDirectory.Entry uriDotFileEntry = new SimpleDirectory.Entry();
		uriDotFileEntry.target = BlobUtil.getBlob(urn);
		uriDotFileEntry.lastModified = new Date().getTime();
		uriDotFileEntry.name = ".ccouch-uri";
		uriDotFileEntry.targetType = CCouchNamespace.TT_SHORTHAND_BLOB;
		uriDotFileEntry.targetSize = ((Blob)uriDotFileEntry.target).getLength();
		((WritableDirectory)dir).addDirectoryEntry(uriDotFileEntry, Collections.EMPTY_MAP);
	}
	
	protected String getCachedDirectoryUrnFromDatabase( File dir ) {
		FileHashCache fac = getFileHashCache(Config.getIdentificationScheme());
		if( fac != null ) {
			String urn = fac.getCachedUrn( (File)dir );
			if( urn != null ) return Config.getRdfSubjectPrefix() + urn;
		}
		return null;
	}
	
	protected void cacheDirectoryUrnInDatabase( File dir, String urn ) {
		FileHashCache fac = getFileHashCache(Config.getIdentificationScheme());
		if( fac != null && fac.isWritable() ) {
			String dataUri = UriUtil.stripRdfSubjectPrefix(urn);
			if( dataUri == null ) {
				Log.log(Log.EVENT_WARNING, "Expected to strip off an RDF subject URI prefix from "+urn+" to cache dir hash, but there apparently wasn't one");
			} else {
				fac.putHashUrn( (File)dir, dataUri );
			}
		}
	}
	
	protected String maybeGetCachedDirectoryUrn( Directory d, Map options ) {
		String urn = null;
		if( MetadataUtil.isEntryTrue(options, CCouchNamespace.REQ_USE_URI_DOT_FILES) ) {
			Directory.Entry uriDotFileEntry = d.getDirectoryEntry(".ccouch-uri");
			if( uriDotFileEntry != null ) {
				Object target = uriDotFileEntry.getTarget();
				if( target instanceof Ref ) target = TheGetter.get( ((Ref)target).getTargetUri() );
				urn = ValueUtil.getString(target);
				if( urn != null ) return urn;
			}
		}
		if( d instanceof File && MetadataUtil.isEntryTrue(options, CCouchNamespace.REQ_CACHE_DIRECTORY_HASHES) ) {
			urn = getCachedDirectoryUrnFromDatabase( (File)d );
		}
		return urn;
	}
	
	
	//// Repository path stuff ////
	
	protected String urnToPostSectorPath( RepoConfig repoConfig, String urn ) {
		if( !repoConfig.storageScheme.couldTranslateUrn(urn) ) return null;
		byte[] hash = repoConfig.storageScheme.urnToHash(urn);
		String filename = repoConfig.storageScheme.hashToFilename(hash);
		return filenameToPostSectorPath(repoConfig, filename);
	}
	
	protected ShortTermCache repoDataSectorUrlCache = new ShortTermCache(60000);
	
	protected List getRepoDataSectorUrlsWithCaching( RepoConfig repoConfig ) {
		List l = (List)repoDataSectorUrlCache.get(repoConfig.uri);
		if( l == null ) {
			l = getRepoDataSectorUrls( repoConfig );
			repoDataSectorUrlCache.put( repoConfig.uri, l );
		}
		return l;
	}
	
	protected List getRepoDataSectorUrls( RepoConfig repoConfig ) {
		ArrayList l = new ArrayList();
		String dataDirUri = PathUtil.appendPath(repoConfig.uri,"data/");
		
		BaseRequest dirReq = new BaseRequest( RequestVerbs.GET, dataDirUri );
		Response dirRes = TheGetter.call(dirReq);
		if( dirRes.getStatus() != ResponseCodes.NORMAL ) return l;
		if( !(dirRes.getContent() instanceof Directory) ) {
			Log.log(Log.EVENT_WARNING, dataDirUri + " exists but is not a directory");
		}
		Directory d = (Directory)dirRes.getContent();
		for( Iterator i=d.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry e = (Directory.Entry)i.next();
			if( CCouchNamespace.TT_SHORTHAND_DIRECTORY.equals(e.getTargetType()) ) {
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
		String ss = ValueUtil.getString(req.getMetadata().get(CCouchNamespace.REQ_STORE_SECTOR));
		if( ss == null ) ss = repoConfig.userStoreSector;
		return ss;
	}
	
	//// PUT stuff ////
	
	// request verb and URI will be ignored for all put* methods
	
	protected Response putDataBlob( RepoConfig repoConfig, Request req ) {
		String sector = getRequestedStoreSector(req, repoConfig);
		
		String idUrn = identifyBlob((Blob)req.getContent(), req.getMetadata());
		
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
		subReq.verb = RequestVerbs.PUT;
		BaseResponse res = new BaseResponse( TheGetter.call(subReq) );
		res.putMetadata(CCouchNamespace.RES_STORED_IDENTIFIER, idUrn);
		res.putMetadata(CCouchNamespace.RES_TREE_FULLY_STORED, Boolean.TRUE);
		Date mtime = (Date)req.getContentMetadata().get(DcNamespace.DC_MODIFIED);
		if( mtime != null ) {
			res.putMetadata(CCouchNamespace.RES_HIGHEST_BLOB_MTIME, mtime);
		}
		return res;
	}

	/**
	 * Does the blob-inserting work for RDF inserting.
	 * Note that request and response are a bit asymmetrical...
	 * @param repoConfig
	 * @param req content = RDF _blob_
	 * @return Response regarding storage of the RDF itself (not the blob)
	 */
	protected Response putDataRdf( RepoConfig repoConfig, Request req ) {
		Object parsedFrom = req.getContentMetadata().get(CCouchNamespace.PARSED_FROM);
		String sourceUri = (String)req.getContentMetadata().get(CCouchNamespace.SOURCE_URI);
		BaseRequest subReq = new BaseRequest();
		subReq.metadata = req.getMetadata();
		String rdfString = ((RdfNode)req.getContent()).toString();
		rdfString = normalizeRdfString( rdfString );
		subReq.content = parsedFrom != null ? parsedFrom : BlobUtil.getBlob(rdfString);
		if( sourceUri != null ) {
			subReq.putContentMetadata(CCouchNamespace.SOURCE_URI, "x-rdfified:" + sourceUri);
		}
		Response blobPutRes = putDataBlob( repoConfig, subReq );
		if( blobPutRes.getStatus() != ResponseCodes.NORMAL ) return blobPutRes;

		String storedUri = (String)blobPutRes.getMetadata().get(CCouchNamespace.RES_STORED_IDENTIFIER);
		String storedRdfUri = storedUri != null ? Config.getRdfSubjectPrefix()+storedUri : null;
		boolean blobFullyStored = ValueUtil.getBoolean(blobPutRes.getMetadata().get(CCouchNamespace.RES_TREE_FULLY_STORED),false);
		
		boolean fullyStored;
		if( storedRdfUri != null && sourceUri != null ) {
			if( ValueUtil.getBoolean(MergeUtil.areUrisEquivalent(sourceUri,storedRdfUri),false) ) {
				fullyStored = blobFullyStored;
				if( !fullyStored ) {
					Log.log(Log.EVENT_WARNING, "blobPutRes (called from putDataRdf) did not return fully stored = true");
				}
			} else if( sourceUri.indexOf(":urn:") != -1 ) {
				// When both are URNs but do not match is the only time we leave
				// fullyStored as false, since something could be amiss, I guess
				fullyStored = false;
				Log.log(Log.EVENT_WARNING, "Expected RDF object to be stored as "+sourceUri+", but stored identifier returned is "+storedRdfUri);
			} else {
				fullyStored = blobFullyStored;
			}
		} else if( sourceUri == null ) {
			fullyStored = blobFullyStored;
		} else {
			fullyStored = false; // We can't really know since there's no URI!
			Log.log(Log.EVENT_WARNING, "No URI returned for stored RDF blob.");
		}
		
		BaseResponse res = new BaseResponse();
		res.putMetadata(CCouchNamespace.RES_STORED_IDENTIFIER, storedRdfUri);
		res.putMetadata(CCouchNamespace.RES_TREE_FULLY_STORED, Boolean.valueOf(fullyStored));
		return res;
	}
	
	protected Response putDataRdfDirectory( RepoConfig repoConfig, Request req ) {
		String treeUri = (String)req.getContentMetadata().get(CCouchNamespace.SOURCE_URI);
		String sector = getRequestedStoreSector(req, repoConfig);

		boolean attemptToSkipFullyStoredTrees =
			ValueUtil.getBoolean( req.getMetadata().get(CCouchNamespace.REQ_SKIP_PREVIOUSLY_STORED_DIRS),true);
		if( attemptToSkipFullyStoredTrees ) {
		    if( treeUri == null ) {
		    	Log.log(Log.EVENT_PERFORMANCE_WARNING, "Can't check for already-fully-stored because no SOURCE_URI was given :<");
		    } else if( sector == null ) {
		    	Log.log(Log.EVENT_PERFORMANCE_WARNING, "Can't check for already-fully-stored because no sector was given :<");
		    } else {
				if( isTreeFullyStored(treeUri, sector) ) {
					Log.log(Log.EVENT_SKIPPED_CACHED, treeUri);
					BaseResponse res = new BaseResponse(ResponseCodes.NORMAL, "Rdf directory and entries already stored", "text/plain");
					res.putMetadata(CCouchNamespace.RES_STORED_IDENTIFIER, treeUri);
					res.putMetadata(CCouchNamespace.RES_TREE_FULLY_STORED, Boolean.TRUE);
					return res;
				}
		    }
		}
		
		boolean fullyStored = true;

		Response putRdfBlobRes = putDataRdf( repoConfig, req );
		TheGetter.getResponseValue(putRdfBlobRes, req);
		String storedTreeUri = (String)putRdfBlobRes.getMetadata().get(CCouchNamespace.RES_STORED_IDENTIFIER);
		fullyStored &= ValueUtil.getBoolean( putRdfBlobRes.getMetadata().get(CCouchNamespace.RES_TREE_FULLY_STORED), false );
		
		Directory d = (Directory)req.getContent();
		for( Iterator i=d.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry e = (Directory.Entry)i.next();
			Object target = e.getTarget();
			BaseRequest targetPutReq = new BaseRequest();
			MetadataUtil.dereferenceTargetToRequest( target, targetPutReq );
			targetPutReq.metadata = req.getMetadata();
			Response targetPutRes = putData( repoConfig, targetPutReq );
			// TODO: I suppose we should check response code or something, too.
			fullyStored &= ValueUtil.getBoolean( targetPutRes.getMetadata().get(CCouchNamespace.RES_TREE_FULLY_STORED), false );
		}
		
		BaseResponse res = new BaseResponse(ResponseCodes.NORMAL, "Rdf directory and entries stored", "text/plain");
		if( storedTreeUri != null && treeUri != null ) {
			if( ValueUtil.getBoolean(MergeUtil.areUrisEquivalent(treeUri,storedTreeUri),false) ) {
				if( fullyStored ) {
					res.putMetadata(CCouchNamespace.RES_TREE_FULLY_STORED, Boolean.TRUE);
					if( sector != null ) {
				    	markTreeFullyStored( treeUri, sector );
					}
				} else {
					Log.log(Log.EVENT_PERFORMANCE_WARNING, treeUri+" was not 'fully stored' S:-/");
				}
			} else {
				Log.log(Log.EVENT_WARNING, "Expected tree be stored as "+treeUri+", but stored identifier returned is "+storedTreeUri);
			}
		}
		res.putMetadata(CCouchNamespace.RES_STORED_IDENTIFIER, storedTreeUri);
		return res;
	}
	
	protected Response putDataNonRdfDirectory( RepoConfig repoConfig, Request req ) {
		Directory d = (Directory)req.getContent();
		String sector = getRequestedStoreSector(req, repoConfig);
		
		String cachedUri = maybeGetCachedDirectoryUrn(d, req.getMetadata());
		
		if( sector != null && cachedUri != null && isTreeFullyStored( cachedUri, sector ) ) {
			BaseResponse res = new BaseResponse(ResponseCodes.NORMAL, cachedUri + "already fully stored - skipping", "text/plain");
			res.putMetadata(CCouchNamespace.RES_STORED_IDENTIFIER, cachedUri);
			return res;
		}
		
		String sourceUri = (String)req.getContentMetadata().get(CCouchNamespace.SOURCE_URI);		
		//(Date)req.getContentMetadata().get(DcNamespace.DC_MODIFIED);
		Date highestMtime = null; // Don't pay attention to directory mtime
		List rdfDirectoryEntries = new ArrayList();
		boolean fullyStored = true;
		for( Iterator i=d.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry e = (Directory.Entry)i.next();
			Object target = e.getTarget();
			String targetSourceUri;
			if( target instanceof Ref ) {
				targetSourceUri = ((Ref)target).getTargetUri();
				BaseRequest targetGetReq = new BaseRequest( "GET", targetSourceUri );
				targetGetReq.metadata = req.getMetadata();
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
			targetPutReq.putContentMetadata(CCouchNamespace.SOURCE_URI, targetSourceUri);
			long entryMtime = e.getLastModified();
			if( entryMtime != -1 ) {
				targetPutReq.putContentMetadata(DcNamespace.DC_MODIFIED, new Date(entryMtime));
			}
			Response targetPutRes = putData( repoConfig, targetPutReq );
			TheGetter.getResponseValue(targetPutRes, targetPutReq);
			fullyStored &= MetadataUtil.isEntryTrue( targetPutRes.getMetadata(), CCouchNamespace.RES_TREE_FULLY_STORED, false );
			Date subHighestMtime = (Date)targetPutRes.getMetadata().get(CCouchNamespace.RES_HIGHEST_BLOB_MTIME);
			if( subHighestMtime != null && (highestMtime == null || subHighestMtime.compareTo(highestMtime) > 0) ) {
				highestMtime = subHighestMtime;
			}
			String targetUri = MetadataUtil.getStoredIdentifier(targetPutRes);
			if( targetUri == null ) throw new RuntimeException("Inserting entry target returned null");
			rdfDirectoryEntries.add(new RdfDirectory.Entry(e, new BaseRef(targetUri)));
		}
		
		RdfDirectory rdfDir = new RdfDirectory();
		rdfDir.setDirectoryEntries(rdfDirectoryEntries);
		
		BaseRequest dirPutReq = new BaseRequest();
		dirPutReq.content = rdfDir;
		dirPutReq.contentMetadata = req.getContentMetadata();
		dirPutReq.metadata = req.getMetadata();
		BaseResponse dirPutRes = new BaseResponse(putDataRdf( repoConfig, dirPutReq ));
		if( highestMtime != null ) {
			dirPutRes.putMetadata(CCouchNamespace.RES_HIGHEST_BLOB_MTIME, highestMtime);
		}
		fullyStored &= MetadataUtil.isEntryTrue( dirPutRes.getMetadata(), CCouchNamespace.RES_TREE_FULLY_STORED, false );
		
		String storedDirUri = MetadataUtil.getStoredIdentifier(dirPutRes);
		if( fullyStored && storedDirUri != null ) markTreeFullyStored( storedDirUri, sector );
		
		boolean oldEnough;
		if( highestMtime == null ) {
			oldEnough = true; // Meh?
		} else {
			Date noNewerThan = (Date)req.getMetadata().get(CCouchNamespace.REQ_DONT_CREATE_URI_DOT_FILES_WHEN_HIGHEST_BLOB_MTIME_GREATER_THAN);
			oldEnough = (noNewerThan == null) ? true : highestMtime.before(noNewerThan); 
		}
		
		if( oldEnough ) {
			maybeCacheDirectoryUrn( d, storedDirUri, req.getMetadata() );
		}
		
		BaseResponse res = new BaseResponse(dirPutRes);
		res.putMetadata( CCouchNamespace.RES_TREE_FULLY_STORED, Boolean.valueOf(fullyStored) );
		return res;
	}
	
	static String REQ_ANCESTOR_DEPTH_MAP = CCouchNamespace.REQ_CACHE_COMMIT_ANCESTORS+"/cachedDepthMap";
	protected void cacheCommitAncestors( RepoConfig repoConfig, Commit c, Request req ) {
		Map cachingCommitDepths = (Map)req.getMetadata().get(REQ_ANCESTOR_DEPTH_MAP);
		if( cachingCommitDepths == null ) {
			req = new BaseRequest(req);
			cachingCommitDepths = new HashMap();
			((BaseRequest)req).putMetadata(REQ_ANCESTOR_DEPTH_MAP, cachingCommitDepths );
		}
		
		String sourceUri = MetadataUtil.getSourceUriOrUnknown(req.getMetadata());
		int depth = ValueUtil.getNumber( MapUtil.getKeyed( req.getMetadata(), CCouchNamespace.REQ_CACHE_COMMIT_ANCESTORS ), 0 ); 
		if( depth > 0 ) {
			Object[] parentz = c.getParents();
			parentLoop: for( int i=0; i<parentz.length; ++i ) {
				if( parentz[i] instanceof Ref ) {
					String parentUri = ((Ref)parentz[i]).getTargetUri();
					if( !UriUtil.isPureContentUri(parentUri)) {
						Log.log(Log.EVENT_WARNING, "Refusing to follow non-content link to parent commit "+parentUri+" from "+sourceUri);
						continue parentLoop;
					}
					
					int pDepth = depth-1;
					
					Integer alreadyCachingAtDepth = (Integer)cachingCommitDepths.get(parentUri);
					// If we've already [started] caching this commit at
					// this depth or greater, then skip it.
					if( alreadyCachingAtDepth != null && alreadyCachingAtDepth.intValue() >= pDepth ) continue;
					cachingCommitDepths.put( parentUri, new Integer(pDepth) );
					
					BaseRequest parentGetReq = new BaseRequest( RequestVerbs.GET, parentUri );
					Response parentRes = TheGetter.call(parentGetReq);
					try {
						AbnormalResponseException.throwIfNonNormal(parentRes,parentGetReq);
						BaseRequest parentPutReq = new BaseRequest();
						parentPutReq.content = parentRes.getContent();
						parentPutReq.contentMetadata = parentRes.getContentMetadata();
						parentPutReq.putContentMetadata(CCouchNamespace.SOURCE_URI, parentUri);
						parentPutReq.metadata = req.getMetadata();
						parentPutReq.putMetadata(CCouchNamespace.REQ_CACHE_COMMIT_ANCESTORS, new Integer(pDepth));
						try {
							AbnormalResponseException.throwIfNonNormal( putData( repoConfig, parentPutReq ), parentPutReq );
						} catch( NotFoundException nfe ) {
							Log.log(Log.EVENT_NOT_FOUND, nfe.getRequest().getResourceName() );
						}
					} catch( NotFoundException nfe ) {
						Log.log(Log.EVENT_NOT_FOUND, "Unable to find "+parentUri+" while caching ancestor commits (depth="+pDepth+")");
						break;
					} catch( AbnormalResponseException e ) {
						Log.log(Log.EVENT_WARNING, "Error while caching commit "+parentUri+": "+e.getMessage());
					}
				} else {
					Log.log(Log.EVENT_WARNING, "Commit "+sourceUri+" parent is not a Ref");
				}
			}
		}
	}
	
	protected boolean shouldStoreCommitTarget( Request req ) {
		return ValueUtil.getBoolean(MapUtil.getKeyed(req.getMetadata(), CCouchNamespace.REQ_CACHE_COMMIT_TARGETS), true);
	}

	protected Response putDataRdfCommit( RepoConfig repoConfig, Request req ) {
		Response putRdfBlobRes = putDataRdf( repoConfig, req );
		TheGetter.getResponseValue(putRdfBlobRes, req);
		
		Commit c = (Commit)req.getContent();
		if( shouldStoreCommitTarget(req) ) {
			Object target = c.getTarget();
			BaseRequest targetPutReq = new BaseRequest();
			targetPutReq.metadata = req.getMetadata();
			if( target instanceof Ref ) {
				String targetGetUri = ((Ref)target).getTargetUri();
				if( !UriUtil.isPureContentUri(targetGetUri)) {
					String sourceUri = MetadataUtil.getSourceUriOrUnknown(req.getMetadata());
					Log.log( Log.EVENT_WARNING, "Refusing to follow non-content link to target "+targetGetUri+" from "+sourceUri );
					return BaseResponse.RESPONSE_NOTFOUND;
				}
			}
			MetadataUtil.dereferenceTargetToRequest( target, targetPutReq );
			putData( repoConfig, targetPutReq );
		}
		
		BaseResponse res = new BaseResponse(ResponseCodes.NORMAL, "Rdf commit and target stored", "text/plain");
		MetadataUtil.copyStoredIdentifier(putRdfBlobRes, res, null);
		
		cacheCommitAncestors( repoConfig, c, req );
		
		return res;
	}
	
	protected Response putDataNonRdfCommit( RepoConfig repoConfig, Request req ) {
		Commit c = (Commit)req.getContent();
		Object target = c.getTarget();
		String targetUri = null;
		if( shouldStoreCommitTarget(req) ) {
			if( target instanceof Ref ) {
				String targetGetUri = ((Ref)target).getTargetUri();
				if( !UriUtil.isPureContentUri(targetGetUri)) {
					String sourceUri = MetadataUtil.getSourceUriOrUnknown(req.getMetadata());
					Log.log( Log.EVENT_WARNING, "Refusing to follow non-content link to target "+targetGetUri+" from "+sourceUri );
					return BaseResponse.RESPONSE_NOTFOUND;
				}
				target = TheGetter.get( targetGetUri );
			}
			BaseRequest targetPutReq = new BaseRequest();
			targetPutReq.metadata = req.getMetadata();
			targetPutReq.content = target;
			Response targetPutRes = putData( repoConfig, targetPutReq );
			targetUri = (String)targetPutRes.getContentMetadata().get(CCouchNamespace.RES_STORED_IDENTIFIER);
			if( targetUri == null ) throw new RuntimeException("Inserting entry target returned null");
		}
		// And what if not?  Then targetUri is null, and this is a problem for the following code...

		RdfCommit rdfDir = new RdfCommit(c, new BaseRef(targetUri));
		
		BaseRequest putRdfCommitReq = new BaseRequest();
		putRdfCommitReq.content = rdfDir;
		putRdfCommitReq.metadata = req.getMetadata();
		Response res = putDataRdf( repoConfig, putRdfCommitReq );
		
		cacheCommitAncestors( repoConfig, c, req );
		
		return res;
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
			return new BaseResponse(ResponseCodes.NORMAL, count + " items inserted", "text/plain");
		} else if( content instanceof Directory.Entry ) {
			BaseRequest subReq = new BaseRequest();
			subReq.metadata = req.getMetadata();
			subReq.content = ((Directory.Entry)content).getTarget();
			return putData( repoConfig, subReq );
		} else {
			throw new RuntimeException("Can't put " + content.getClass().getName() + " to repo data store");
		}
	}
	
	//// Identify stuff ////
	
	protected String identifyBlob( Blob blob, Map options ) {
		if( !MetadataUtil.isEntryTrue(options, CCouchNamespace.REQ_DONT_CACHE_FILE_HASHES) ) {
			FileHashCache fac = getFileHashCache(Config.getIdentificationScheme());
			if( fac != null && blob instanceof FileBlob ) {
				return fac.getUrn((FileBlob)blob);
			}
		}
		String hash = Config.getIdentificationScheme().hashToUrn(Config.getIdentificationScheme().getHash(blob));
		if( blob.getLength() == -1 || blob.getLength() > 4096 ) {
			Log.log(Log.EVENT_PERFORMANCE_WARNING, "Unable to cache URN of "+blob.getClass().getName()+" ("+hash+", size="+blob.getLength()+")");
		}
		return hash;
	}
		
	/**
	 * Note that this should only be called by identify(Object,Map,Map)
	 * if the target is not already an RDFNode, so this function doesn't
	 * worry about the 'don't need to do any work to identify this' optimization.  
	 */
	protected String identifyDirectory( Directory dir, Map options ) {
		String urn = maybeGetCachedDirectoryUrn(dir, options);
		if( urn != null ) return urn;
		
		urn = identify( new RdfDirectory( (Directory)dir, getTargetRdfifier(false, false, options) ), Collections.EMPTY_MAP, options );
		maybeCacheDirectoryUrn(dir, urn, options);
		
		return urn;
	}
	
	protected String identify( Object content, Map contentMetadata, Map options ) {
		if( content instanceof RdfNode ) {
			Object parsedFrom = contentMetadata.get(CCouchNamespace.PARSED_FROM);
			if( parsedFrom == null ) {
				Log.log(Log.EVENT_PERFORMANCE_WARNING, "Blobbifying RDF node just to get content URN");
				String rdfString = normalizeRdfString( content.toString() );
				parsedFrom = BlobUtil.getBlob( rdfString );
			}
			String parsedFromUri = identify( parsedFrom, Collections.EMPTY_MAP, options );
			// TODO: Make sure repoConfig.dataScheme.wouldHandleUrn(parsedFromUri)?
			return Config.getRdfSubjectPrefix() + parsedFromUri;
		} else if( content instanceof Commit ) {
			return identify( new RdfCommit( (Commit)content, getTargetRdfifier(false, false, options) ), contentMetadata, options );
		} else if( content instanceof Directory ) {
			return identifyDirectory( (Directory)content, options );
		} else if( content instanceof Ref && Config.getIdentificationScheme().couldTranslateUrn(((Ref)content).getTargetUri()) ) {
			return ((Ref)content).getTargetUri();
		} else {
			content = TheGetter.dereference(content);
			Blob blob = BlobUtil.getBlob(content, false);
			if( blob != null ) return identifyBlob( blob, options );
		}
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
				slf.initIfEmpty(65536, 1024*1024);
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
	
	protected SimpleListFile getCacheSlf( String cacheName ) {
		if( config.defaultRepoConfig.uri.startsWith("file:") ) {
			return getSlf( config.defaultRepoConfig, "cache/"+cacheName+".slf" );
		}
		return null;
	}
	
	protected void cacheString( String cacheName, String valueName, String value ) {
		if( stringCacheOverride != null ) {
			stringCacheOverride.put(cacheName+"/"+valueName, value);
			return;
		}
		
		SimpleListFile slf = getCacheSlf( cacheName );
		try {
			if( slf != null ) slf.put(valueName, ValueUtil.getBytes(value));
		} catch( IOException e ) {
			Log.log(Log.EVENT_ERROR, "IOError while putting '"+valueName+"' into '"+cacheName+"' cache: "+e.getMessage());
		}
	}
	
	protected String getCachedString( String cacheName, String valueName ) {
		if( stringCacheOverride != null ) {
			return stringCacheOverride.get(cacheName+"/"+valueName).toString();
		}
		
		SimpleListFile slf = getCacheSlf( cacheName );
		if( slf == null ) return null;
		try {
			byte[] v = slf.get(valueName);
			if( v != null ) return ValueUtil.getString(v);
		} catch( IOException e ) {
			Log.log(Log.EVENT_ERROR, "IOError while getting '"+valueName+"' from '"+cacheName+"' cache: "+e.getMessage());
		}
		return null;
	}
	
	protected void markTreeFullyStored( String uri, String sector ) {
		cacheString("fully-cached-trees/"+sector, uri, String.valueOf(new Date().getTime()) );
	}
	
	protected boolean isTreeFullyStored( String uri, String sector ) {
		return getCachedString("fully-cached-trees/"+sector, uri ) != null;
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
		this.stringCacheOverride = config.stringCacheOverride;
	}
	
	protected RepoConfig lastHitRepoConfig;
	protected String lastHitDataSectorUri;
	
	//// Validate and cache blobs fetched from remote repos ////
	
	protected static class HashMismatchException extends Exception {
		private static final long serialVersionUID = 1L;
		
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
		String cs = (String)req.getMetadata().get(CCouchNamespace.REQ_CACHE_SECTOR);
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
				if( cas.canVerifyUrn(urn) ) {
					if( !cas.verify(urn, blob) ) {
						throw new HashMismatchException( cas, cas.urnToHash(urn), cas.getHash(blob) );
					}
					verified = true;
				}
			}
			if( !verified ) {
				throw new RuntimeException("No ContentAddressingScheme available to verify "+urn);
			}
			
			// Otherwise it's all good...we'll store the blob
			BaseRequest putReq = new BaseRequest(req);
			putReq.verb = RequestVerbs.PUT;
			putReq.uri = "data"; // Will not be used...
			putReq.content = res.getContent();
			putReq.contentMetadata = res.getContentMetadata();
			putReq.metadata = req.getMetadata();
			putReq.putMetadata(CCouchNamespace.REQ_STORE_SECTOR, cs);
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
	
	ShortTermCache nonExistentObjects = new ShortTermCache(60000);
		
	public Response call( Request req ) {
		if( "x-ccouch-repo://".equals(req.getResourceName()) ) {
			// Return a directory listing of all registered repositories
			SimpleDirectory sd = new SimpleDirectory();
			for( Iterator i=this.config.namedRepoConfigs.values().iterator(); i.hasNext(); ) {
				RepoConfig repoConfig = (RepoConfig)i.next();
				SimpleDirectory.Entry entry = new SimpleDirectory.Entry();
				entry.name = repoConfig.name;
				entry.targetType = CCouchNamespace.TT_SHORTHAND_DIRECTORY;
				entry.target = new BaseRef("x-ccouch-repo:all-repos-dir", entry.name + "/", repoConfig.uri);
				sd.addDirectoryEntry(entry, Collections.EMPTY_MAP);
			}
			return new BaseResponse(ResponseCodes.NORMAL, sd);
		} else if( req.getResourceName().startsWith("x-ccouch-head:") || req.getResourceName().startsWith("x-ccouch-repo:") ) {
			req = MFArgUtil.argumentizeQueryString(req);
			RepoRef repoRef = RepoRef.parse(req.getResourceName(), false);
			RepoConfig repoConfig;
			if( repoRef.repoName == null ) {
				repoConfig = config.defaultRepoConfig;
				if( repoConfig == null ) {
					BaseResponse res = new BaseResponse(ResponseCodes.DOES_NOT_EXIST, "No default repository to handle " + req.getResourceName());
					res.putContentMetadata(DcNamespace.DC_FORMAT, "text/plain");
					return res;
				}
			} else {
				repoConfig = (RepoConfig)config.namedRepoConfigs.get(repoRef.repoName);
				if( repoConfig == null ) {
					BaseResponse res = new BaseResponse(ResponseCodes.DOES_NOT_EXIST, "No such repository: " + repoRef.repoName);
					res.putContentMetadata(DcNamespace.DC_FORMAT, "text/plain");
					return res;
				}
			}
			
			if( "identify".equals(repoRef.subPath) ) {
				return new BaseResponse( ResponseCodes.NORMAL, identify( MFArgUtil.getPrimaryArgument(req.getContent()), req.getContentMetadata(), req.getMetadata() ), "text/plain" );
			}
			
			if( RequestVerbs.PUT.equals(req.getVerb()) || RequestVerbs.POST.equals(req.getVerb()) ) {
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
					return new BaseResponse( ResponseCodes.NORMAL, "OK" );
				} else if( repoRef.subPath.equals("data") || repoRef.subPath.startsWith("data/") ) {
					String[] dataAndSector = repoRef.subPath.split("/");
					if( dataAndSector.length > 1 && dataAndSector[1].length() > 0 ) {
						BaseRequest sectorOverrideReq = new BaseRequest(req);
						sectorOverrideReq.putMetadata(CCouchNamespace.REQ_STORE_SECTOR, dataAndSector[1]);
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
					if( subRes.getStatus() == ResponseCodes.NORMAL ) {
						return subRes;
					}
					
					// If that fails, try getting initial parts before '/'
					// and parsing them
					while( headPath.lastIndexOf('/') != -1 ) {
						headPath = headPath.substring(0, headPath.lastIndexOf('/'));
						resolvedHeadPath = resolveHeadPath(repoConfig, headPath, "");
						subReq = new BaseRequest(req, resolvedHeadPath);
						subRes = TheGetter.call(subReq);
						if( subRes.getStatus() == ResponseCodes.NORMAL ) {
							String headContent = ValueUtil.getString( subRes.getContent() );
							Object head = RdfIO.parseRdf(headContent, resolvedHeadPath);
							return FollowPath.followPath( req, head, UriUtil.uriDecode(inputHeadPath.substring(headPath.length()+1)));
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
					
					return new BaseResponse(ResponseCodes.NORMAL, getFunctionResult(repoConfig, subIndexName, key));
				} else if( path.equals("") ) {
					SimpleDirectory sd = new SimpleDirectory();
					sd.addDirectoryEntry(new SimpleDirectory.Entry("files",new BaseRef(req.getResourceName(),"files"),CCouchNamespace.TT_SHORTHAND_DIRECTORY));
					sd.addDirectoryEntry(new SimpleDirectory.Entry("heads",new BaseRef(req.getResourceName(),"heads"),CCouchNamespace.TT_SHORTHAND_DIRECTORY));
					sd.addDirectoryEntry(new SimpleDirectory.Entry("parsed-heads",new BaseRef(req.getResourceName(),"parsed-heads"),CCouchNamespace.TT_SHORTHAND_DIRECTORY));
					return new BaseResponse( 200, sd );
				}
				BaseRequest subReq = new BaseRequest(req, path);
				return TheGetter.call(subReq);
			}
			
			//String sector = MetadataUtil.getKeyed(request.getMetadata(), RdfNamespace.STORE_SECTOR, rc.userStoreSector);
		} else {
			if( RequestVerbs.GET.equals(req.getVerb()) || RequestVerbs.HEAD.equals(req.getVerb()) ) {
				// URN request? Check each repo to see if it has a data scheme that would handle it

				String urn = req.getResourceName();
				if( !urn.startsWith("urn:") ) return BaseResponse.RESPONSE_UNHANDLED;
			
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
						if( res.getStatus() == ResponseCodes.NORMAL ) return res;
					}
				}
				
				// Check most recently hit remote repo
				lastHit: if( lastHitDataSectorUri != null ) {
					String psp = urnToPostSectorPath(lastHitRepoConfig, urn);
					if( psp == null ) break lastHit;
					
					BaseRequest subReq = new BaseRequest(req, PathUtil.appendPath(lastHitDataSectorUri, psp));
					Response res = TheGetter.call(subReq);
					if( res.getStatus() == ResponseCodes.NORMAL ) {
						try {
							return maybeCacheBlob(res, req);
						} catch( HashMismatchException hme ) {
							logHashMismatch( subReq, hme.dataScheme, hme.expected, hme.calculated );
						}
					}
				}
				
				// Check all remote repos (unless we are explicitly asked not to)
				checkRemote: if( !MetadataUtil.isEntryTrue(req.getMetadata(), CCouchNamespace.REQ_LOCAL_REPOS_ONLY)) {
					if( nonExistentObjects.get(urn) != null ) break checkRemote;
					
					for( Iterator i=config.remoteRepoConfigs.iterator(); i.hasNext(); ) {
						RepoConfig repoConfig = (RepoConfig)i.next();
						String psp = urnToPostSectorPath(repoConfig, urn);
						if( psp == null ) continue;
		
						List dataSectorUris = getRepoDataSectorUrlsWithCaching(repoConfig);
						for( Iterator si=dataSectorUris.iterator(); si.hasNext(); ) {
							String dataSectorUri = (String)si.next();
							BaseRequest subReq = new BaseRequest(req, PathUtil.appendPath(dataSectorUri, psp));
							Response res = TheGetter.call(subReq);
							if( res.getStatus() == ResponseCodes.NORMAL ) {
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
					nonExistentObjects.put(urn, urn);
				}
				
				return BaseResponse.RESPONSE_NOTFOUND;
			}
		}
		return BaseResponse.RESPONSE_UNHANDLED;
	}
	
	public Function1 getTargetRdfifier(final boolean nested, final boolean followRefs, final Map options ) {
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
						String id = identify(rdfDir, Collections.EMPTY_MAP, options);
						return new BaseRef(ValueUtil.getString(id));
					}
				} else if( input instanceof Ref ) {
					return input;
				} else if( input instanceof Blob ) {
					return new BaseRef(identifyBlob((Blob)input, options));
				} else {
					throw new RuntimeException("Don't know how to rdf-ify " + input.getClass().getName() );
				}
			}
		};
	}
}
