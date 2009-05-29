package contentcouch.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import togos.rra.BaseRequest;
import togos.rra.BaseRequestHandler;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.path.PathUtil;
import contentcouch.rdf.DcNamespace;
import contentcouch.store.TheGetter;

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
	
	protected String getDataPostSectorPath( RepoConfig repoConfig, String urn ) {
		if( !repoConfig.dataScheme.wouldHandleUrn(urn) ) return null;
		byte[] hash = repoConfig.dataScheme.urnToHash(urn);
		String filename = repoConfig.dataScheme.hashToFilename(hash);
		
		if( filename.length() >= 2 ) {
			return filename.substring(0,2) + "/" + filename;
		} else {
			return filename;
		}
	}
	
	protected List getRepoDataSectorUrls( RepoConfig repoConfig ) {
		ArrayList l = new ArrayList();
		// TODO: actually find them!
		l.add(PathUtil.appendPath(repoConfig.uri,"data/user/"));
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
			
			// TODO: if posting to //repo/new-data, handle specially
			
			BaseRequest subReq = new BaseRequest(req, repoConfig.uri + repoRef.subPath);
			return TheGetter.handleRequest(subReq);
			
			//String sector = MetadataUtil.getKeyed(request.getMetadata(), RdfNamespace.STORE_SECTOR, rc.userStoreSector);
		} else {
			String urn = req.getUri(); 
			for( Iterator i=config.getAllRepoConfigs().iterator(); i.hasNext(); ) {
				RepoConfig repoConfig = (RepoConfig)i.next();
				String psp = getDataPostSectorPath(repoConfig, urn);
				if( psp == null ) continue;

				if( "GET".equals(req.getVerb()) ) {
					List dataSectorUris = getRepoDataSectorUrls(repoConfig);
					for( Iterator si=dataSectorUris.iterator(); si.hasNext(); ) {
						String dataSectorUri = (String)si.next();
						BaseRequest subReq = new BaseRequest(req, PathUtil.appendPath(dataSectorUri, psp));
						Response res = TheGetter.handleRequest(subReq);
						if( res.getStatus() == Response.STATUS_NORMAL ) return res;
					}
				} else {
					// TODO: Handle HEADs or EXISTs or whatever.
				}
			}
			// Check if any repo stores data using a matching URI scheme, such as urn:sha1:
		}
		// TODO Auto-generated method stub
		return BaseResponse.RESPONSE_UNHANDLED;
	}
}
