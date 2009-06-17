package contentcouch.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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
	protected static class RepoRef {
		String repoName, subPath;
		
		public RepoRef( String repoName, String subPath ) {
			this.repoName = repoName;
			this.subPath = subPath;
		}
		
		public static RepoRef parse(String uri, boolean assumeHead) {
			String repoName = null;
			String subPath = (uri.startsWith("x-ccouch-head:") || assumeHead) ?
				"heads/" : "";
			if( uri.startsWith("x-ccouch-head:") ) {
				uri = uri.substring("x-ccouch-head:".length());
			} else {
				subPath = "";
			}
			if( uri.startsWith("x-ccouch-repo:") ) {
				uri = uri.substring("x-ccouch-repo:".length());
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
			subPath += uri;
			return new RepoRef(repoName, subPath);
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
			System.err.println("Using file hash cache "+ hashCachePath);
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
	
	protected Response put( Request req, RepoConfig repoConfig, String path, Object value ) {
		BaseRequest subReq = new BaseRequest(req);
		subReq.content = value;
		subReq.clearContentMetadata();
		return put( subReq, repoConfig, path );
	}
	
	protected Response put( Request req, RepoConfig repoConfig, String path ) {
		Blob blob;
		if( req.getContent() instanceof Directory.Entry ) {
			return put( req, repoConfig, path, ((Directory.Entry)req.getContent()).getTarget() );
		} else if( req.getContent() instanceof Iterator ) {
			int count = 0;
			for( Iterator i=(Iterator)req.getContent(); i.hasNext(); ) {
				put( req, repoConfig, path, i.next() );
				++count;
			}
			return new BaseResponse(Response.STATUS_NORMAL, count + " items inserted at " + repoConfig.uri + path);
		} else if( req.getContent() instanceof Collection ) {
			return put( req, repoConfig, path, ((Collection)req.getContent()).iterator() );
		} else if( req.getContent() instanceof RdfNode ) {
			Response blobPutRes = put(req, repoConfig, path, BlobUtil.getBlob(req.getContent().toString()));
			BaseResponse res = new BaseResponse(blobPutRes);
			MetadataUtil.copyStoredIdentifier(blobPutRes, res, "x-parse-rdf:");
			return res;
		} else if( req.getContent() instanceof Commit ) {
			return put( req, repoConfig, path, new RdfCommit((Commit)req.getContent(), getStoringTargetRdfifier(true, req, repoConfig)) );
		} else if( req.getContent() instanceof Directory ) {
			return put( req, repoConfig, path, new RdfDirectory((Directory)req.getContent(), getStoringTargetRdfifier(true, req, repoConfig)) );
		} else if( (blob = BlobUtil.getBlob(req.getContent(), false)) != null ) {
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
				subReq.putMetadata(CcouchNamespace.REQ_FILEMERGE_METHOD, CcouchNamespace.REQ_FILEMERGE_IGNORE);
				BaseResponse res = new BaseResponse( TheGetter.handleRequest(subReq) );
				res.putMetadata(CcouchNamespace.RES_STORED_IDENTIFIER, repoConfig.dataScheme.hashToUrn(hash));
				return res;
			} else if( path.startsWith("heads/") ) {
				Response dataPutRes = put( req, repoConfig, "data" );	// Put blob in data, too
				BaseRequest subReq = new BaseRequest( req, resolveHeadPath( repoConfig, path, "new") );
				BaseResponse res = new BaseResponse(TheGetter.handleRequest(subReq));
				MetadataUtil.copyStoredIdentifier(dataPutRes, res, null);
				return res;
			} else {
				throw new RuntimeException( "Can't PUT to " + req.getUri() + ": unrecognised post-repo path" );
			}
		} else if( req.getContent() == null ) {
			throw new RuntimeException( "PUT/POST to " + req.getUri() + " requires content, null given.");
		} else {
			throw new RuntimeException( "Don't know how to PUT/POST " + req.getContent().getClass().getName() );
		}
	}
	
	protected String identifyBlob( Blob blob, RepoConfig repoConfig ) {
		return repoConfig.dataScheme.hashToUrn(getHash(blob));
	}
	
	protected Response identify( Request req, Object obj, RepoConfig repoConfig ) {
		if( obj instanceof RdfNode ) {
			Object parsedFrom = (req == null) ? null : req.getContentMetadata().get(CcouchNamespace.PARSED_FROM);
			if( parsedFrom == null ) parsedFrom = BlobUtil.getBlob( obj.toString() );
			String parsedFromUri = ValueUtil.getString(identify( null, parsedFrom, repoConfig ).getContent());
			return new BaseResponse(Response.STATUS_NORMAL,	"x-parse-rdf:" + parsedFromUri, "text/plain");
		} else if( obj instanceof Commit ) {
			return identify( req, new RdfCommit( (Commit)obj, getTargetRdfifier(false, false) ), repoConfig );
		} else if( obj instanceof Directory ) {
			return identify( req, new RdfDirectory( (Directory)obj, getTargetRdfifier(false, false) ), repoConfig );
		}

		Blob blob = BlobUtil.getBlob(obj, false);
		if( blob != null ) return new BaseResponse(Response.STATUS_NORMAL, identifyBlob( blob, repoConfig ), "text/plain");
		
		throw new RuntimeException("I don't know how to identify " + obj.getClass().getName());
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
					return identify( req, req.getContent(), repoConfig );
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
						return new BaseRef(identifyBlob(BlobUtil.getBlob(rdfDir.toString()), config.defaultRepoConfig));
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
			public Object apply(Object input) {
				if( followRefs && input instanceof Ref ) {
					String targetUri = ((Ref)input).getTargetUri();
					if( targetUri.startsWith("x-parse-rdf:") || targetUri.startsWith("x-parse-rdf:urn:") ) {
						// Then leave as is
					} else {
						input = TheGetter.get(targetUri);
					}
				}

				if( input == null ) {
					return null;
				} else if( input instanceof Ref ) {
					return input;
				} else {
					Response res = put( req, repoConfig, "data", input );
					String storedUri = MetadataUtil.getStoredIdentifier(res);
					if( storedUri == null ) throw new RuntimeException("Identifier not returned when storing " + input.getClass().getName() );
					return new BaseRef( storedUri );
				}
			}
		};
	}
}
