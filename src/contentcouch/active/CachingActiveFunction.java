package contentcouch.active;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import contentcouch.active.expression.Expression;
import contentcouch.blob.Blob;
import contentcouch.framework.TheGetter;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.value.BaseRef;
import contentcouch.value.Ref;

import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;

public abstract class CachingActiveFunction extends BaseActiveFunction {
	/**
	 * Will be called when there is no cached result, and we need to generate one.
	 */
	protected abstract Object _getResult( Request req, Map canonArgExpressions );
	/**
	 * Returns a map of all and only the argument expressions that are actually used.
	 * Defaults should be filled in when the given argumentExpressions are missing or invalid.
	 * @param argumentExpressions input expressions
	 * @return map of canonical argument expressions based on the input ones
	 */
	protected abstract Map getCanonicalArgumentExpressions( Map argumentExpressions );
	/** Returns the name of the index where result URIs should be stored */
	protected abstract String getCacheIndexName( Request req, Map canonArgExpressions );
	
	/** Returns a string that would uniquely identify the result of this calculation. */ 
	protected String _getCacheKey( Request req, Map canonArgExpressions ) {
		return this.toCallExpression(canonArgExpressions).toUri();
	}
		
	protected String getCacheKey( Request req, Map argumentExpressions ) {
		return _getCacheKey(req, getCanonicalArgumentExpressions(argumentExpressions));
	}
	
	/** Returns true iff all canonical argument expressions are constant */
	public boolean isConstant( Map argumentExpressions ) {
		Map ae = getCanonicalArgumentExpressions( argumentExpressions );
		for( Iterator i=ae.values().iterator(); i.hasNext(); ) {
			if( !((Expression)i.next()).isConstant() ) return false;
		}
		return true;
	}
	
	public Response call(Request req, Map argumentExpressions) {
		Map canonArgExpressions = getCanonicalArgumentExpressions(argumentExpressions);
		boolean cacheable = this.isConstant( canonArgExpressions ); 
		String resultCacheUri = "x-ccouch-repo:function-result-cache/"+getCacheIndexName(req,canonArgExpressions)+"/"+_getCacheKey(req,canonArgExpressions);
		Object cached = cacheable ? TheGetter.get(resultCacheUri) : null;
		String thumbnailUri;
		if( cached == null ) {
			// Should never default to 32x32, as canonical arguments should
			// default to 64x64
			Blob output = (Blob)_getResult(req, canonArgExpressions);
			
			if( cacheable ) {
				BaseRequest storeReq = new BaseRequest(RequestVerbs.POST, "x-ccouch-repo:data", output, Collections.EMPTY_MAP);
				storeReq.putMetadata( CCouchNamespace.REQ_STORE_SECTOR, "function-results" );
				storeReq.putMetadata( CCouchNamespace.REQ_FILEMERGE_METHOD, CCouchNamespace.REQ_FILEMERGE_IGNORE );
				Response storeRes = TheGetter.call(storeReq);
				thumbnailUri = (String)storeRes.getMetadata().get(CCouchNamespace.RES_STORED_IDENTIFIER);
				
				TheGetter.put(resultCacheUri, new BaseRef(thumbnailUri));
			} else {
				return new BaseResponse( ResponseCodes.NORMAL, output );
			}
		} else {
			if( cached instanceof Ref ) { // oughta be
				thumbnailUri = ((Ref)cached).getTargetUri();
			} else {
				throw new RuntimeException("Object returned from cache was not ref: " + cached);
			}
		}
		if( thumbnailUri == null ) {
			throw new RuntimeException( "No thumbnailUri" );
		}
		
		BaseResponse subRes = new BaseResponse( TheGetter.call( new BaseRequest( RequestVerbs.GET, thumbnailUri ) ) );
		if( cacheable ) subRes.putMetadata( CCouchNamespace.RES_CACHEABLE, Boolean.TRUE );
		return subRes;
		
		//return new BaseResponse(ResponseCodes.NORMAL, "Thumbnail of "+id, "text/plain");
	}
}
