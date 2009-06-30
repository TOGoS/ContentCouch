package contentcouch.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
	
	public byte[] getHash( Blob blob ) {
		if( blob instanceof FileBlob ) {
			FileHashCache fileHashCache = getFileHashCache();
			return fileHashCache.getHash((FileBlob)blob, config.defaultRepoConfig.dataScheme);
		}
		return config.defaultRepoConfig.dataScheme.getHash( blob );
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

	protected Response putRdf( RepoConfig repoConfig, String path, RdfNode value, Map valueMetadata, Map requestMetadata ) {
		Object parsedFrom = valueMetadata.get(CcouchNamespace.PARSED_FROM);
		Response blobPutRes = put( repoConfig, path, parsedFrom != null ? parsedFrom : BlobUtil.getBlob(value.toString()), requestMetadata );
		BaseResponse res = new BaseResponse(blobPutRes);
		MetadataUtil.copyStoredIdentifier(blobPutRes, res, "x-parse-rdf:");
		return res;
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
		subReq.putMetadata(CcouchNamespace.REQ_DIRMERGE_METHOD, CcouchNamespace.REQ_DIRMERGE_MERGE);
		subReq.putMetadata(CcouchNamespace.REQ_FILEMERGE_METHOD, CcouchNamespace.REQ_FILEMERGE_STRICTIG);
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
			RdfNode storedRdf = new RdfCommit((Commit)content, getStoringTargetRdfifier(true, req, repoConfig));
			return putRdf( repoConfig, path, content instanceof RdfNode ? (RdfNode)content : storedRdf, req.getContentMetadata(), req.getMetadata() );
		} else if( content instanceof Directory ) {
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
					});
					BaseResponse res = new BaseResponse(Response.STATUS_NORMAL, "Directory leaf blobs inserted to data", "text/plain");
					res.putMetadata(CcouchNamespace.RES_STORED_OBJECT, storedDirectory);
					return res;
				} else {
					RdfNode storedRdf = new RdfDirectory((Directory)content, getStoringTargetRdfifier(true, req, repoConfig));
					return putRdf( repoConfig, path, content instanceof RdfNode ? (RdfNode)content : storedRdf, req.getContentMetadata(), req.getMetadata() );
				}
			} else if( path.startsWith("heads/") ) {
				return putHead( repoConfig, path, content, req.getContentMetadata(), req.getMetadata() );
			} else {
				throw new RuntimeException( "Can't PUT to " + req.getUri() + ": unrecognised post-repo path" );
			}
		} else if( content instanceof RdfNode ) {
			return putRdf( repoConfig, path, (RdfNode)content, req.getContentMetadata(), req.getMetadata() );
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
	
	protected String identifyBlob( Blob blob, RepoConfig repoConfig ) {
		return repoConfig.dataScheme.hashToUrn(getHash(blob));
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
				} else {
					if( repoRef.subPath.startsWith("data/") ) {
						String ss = repoRef.subPath.substring(5);
						if( ss.endsWith("/") ) ss = ss.substring(0,ss.length()-1);
						if( ss != null ) {
							req = new BaseRequest( req );
							((BaseRequest)req).putMetadata(CcouchNamespace.REQ_STORE_SECTOR, ss);
						}
					}
					return put( req, repoConfig, repoRef.subPath );
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
			String urn = req.getUri(); 
			for( Iterator i=config.getAllRepoConfigs().iterator(); i.hasNext(); ) {
				RepoConfig repoConfig = (RepoConfig)i.next();
				String psp = urnToPostSectorPath(repoConfig, urn);
				if( psp == null ) continue;

				if( Request.VERB_GET.equals(req.getVerb()) || Request.VERB_HEAD.equals(req.getVerb()) ) {
					List dataSectorUris = getRepoDataSectorUrls(repoConfig);
					for( Iterator si=dataSectorUris.iterator(); si.hasNext(); ) {
						String dataSectorUri = (String)si.next();
						BaseRequest subReq = new BaseRequest(req, PathUtil.appendPath(dataSectorUri, psp));
						Response res = TheGetter.handleRequest(subReq);
						if( res.getStatus() == Response.STATUS_NORMAL ) return res;
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

	public Function1 getStoringTargetRdfifier(final boolean followRefs, final Request req, final RepoConfig repoConfig) {
		return new Function1() {
			public Object apply(Object value) {
				Map valueMetadata;
				if( followRefs && value instanceof Ref ) {
					String targetUri = ((Ref)value).getTargetUri();

					BaseRequest subReq = new BaseRequest(Request.VERB_GET, targetUri);
					Response subRes = TheGetter.handleRequest(subReq);
					value = TheGetter.getResponseValue(subRes, subReq);
					
					if( targetUri.startsWith("x-parse-rdf:") && subRes.getContentMetadata().get(CcouchNamespace.PARSED_FROM) == null ) {
						throw new RuntimeException(targetUri + " not parsed from anywhere, apparently!");
					}
					
					valueMetadata = subRes.getContentMetadata();
				} else {
					valueMetadata = Collections.EMPTY_MAP;
				}

				if( value == null ) {
					return null;
				} else if( value instanceof Ref ) {
					return value;
				} else {
					Response res = put( repoConfig, "data", value, valueMetadata, req.getMetadata() );
					String storedUri = MetadataUtil.getStoredIdentifier(res);
					if( storedUri == null ) throw new RuntimeException("Identifier not returned when storing " + value.getClass().getName() );
					return new BaseRef( storedUri );
				}
			}
		};
	}
}
