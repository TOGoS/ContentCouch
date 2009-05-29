package contentcouch.repository;

import togos.rra.BaseRequestHandler;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.rdf.DcNamespace;

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
	
	MetaRepoConfig config;
	
	public MetaRepository( MetaRepoConfig config ) {
		this.config = config;
	}
	
	public Response handleRequest(Request request) {
		if( request.getUri().startsWith("x-ccouch-head:") || request.getUri().startsWith("x-ccouch-repo:") ) {
			RepoRef r = RepoRef.parse(request.getUri(), false);
			RepoConfig rc;
			if( r.repoName == null ) {
				rc = config.defaultRepoConfig;
				if( rc == null ) {
					BaseResponse res = new BaseResponse(Response.STATUS_DOESNOTEXIST, "No default repository to handle " + request.getUri());
					res.putContentMetadata(DcNamespace.DC_FORMAT, "text/plain");
					return res;
				}
			} else {
				rc = (RepoConfig)config.namedRepoConfigs.get(r.repoName);
				if( rc == null ) {
					BaseResponse res = new BaseResponse(Response.STATUS_DOESNOTEXIST, "No such repository: " + r.repoName);
					res.putContentMetadata(DcNamespace.DC_FORMAT, "text/plain");
					return res;
				}
			}
			
			//String sector = MetadataUtil.getKeyed(request.getMetadata(), RdfNamespace.STORE_SECTOR, rc.userStoreSector);
		} else {
			
			// Check if any repo stores data using a matching URI scheme, such as urn:sha1:
		}
		// TODO Auto-generated method stub
		return BaseResponse.RESPONSE_UNHANDLED;
	}
}
