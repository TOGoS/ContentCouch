package contentcouch.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import togos.rra.BaseRequest;
import togos.rra.BaseRequestHandler;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.blob.BlobUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.rdf.DcNamespace;
import contentcouch.store.TheGetter;
import contentcouch.value.Blob;
import contentcouch.value.Directory;

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
	
	public byte[] getHash( Blob blob ) {
		// TODO: Use file hash cache
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
		Directory d = TheGetter.getDirectory( dataDirUri );
		for( Iterator i=d.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry e = (Directory.Entry)i.next();
			if( CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(e.getTargetType()) ) {
				l.add(PathUtil.appendPath(dataDirUri, e.getKey() + "/"));
			}
		}
		return l;
	}
	
	MetaRepoConfig config;
	
	public MetaRepository( MetaRepoConfig config ) {
		this.config = config;
	}
	
	public Response handleRequest( Request req ) {
		if( req.getUri().startsWith("x-ccouch-head:") || req.getUri().startsWith("x-ccouch-repo:") ) {
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
				Blob blobToPut = BlobUtil.getBlob(req.getContent());
				if( blobToPut == null ) {
					throw new RuntimeException("Can't PUT/POST without content: " + req.getUri());
				}
				
				
				
				if( "identify".equals(repoRef.subPath) ) {
					String urn = repoConfig.dataScheme.hashToUrn(getHash(blobToPut));
					return new BaseResponse(Response.STATUS_NORMAL, urn, "text/plain");
				} else if( repoRef.subPath.startsWith("data") ) {
					// subPath can be
					// data - post data to user store sector
					// data/<sector> - post data to named sector
					String[] pratz = repoRef.subPath.split("/");
					String sector;
					if( pratz.length >= 2 ) sector = pratz[1];
					else sector = repoConfig.userStoreSector;
					
					byte[] hash = repoConfig.dataScheme.getHash(blobToPut);
					String psp = hashToPostSectorPath(repoConfig, hash);
					String uri = repoConfig.uri + "data/" + sector + "/" + psp; 
					
					BaseRequest subReq = new BaseRequest( req, uri );
					return TheGetter.handleRequest(subReq);
				} else {
					return new BaseResponse( Response.STATUS_DOESNOTEXIST, "Can't PUT to " + req.getUri(), "text/plain");
				}
			} else {
				BaseRequest subReq = new BaseRequest(req, repoConfig.uri + repoRef.subPath);
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
}
