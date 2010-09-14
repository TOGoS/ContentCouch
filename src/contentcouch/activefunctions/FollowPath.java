package contentcouch.activefunctions;

import java.util.Map;
import java.util.TreeMap;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import contentcouch.active.ActiveUtil;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.FunctionCallExpression;
import contentcouch.active.expression.Expression;
import contentcouch.active.expression.ValueExpression;
import contentcouch.framework.TheGetter;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathSimplifiableActiveFunction;
import contentcouch.path.PathSimplifiableExpression;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.value.Commit;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class FollowPath extends BaseActiveFunction implements PathSimplifiableActiveFunction {
	/** @todo: Fix this so it passes back response/content metadata
	 * by using TheGetter.call instead of TheGetter.get */
	public static Response followPath( Request req, Object source, String path ) {
		String resolvedUri = null;
		String[] pathParts = path.split("/+");
		for( int i=0; i<pathParts.length; ++i ) {
			if( source instanceof Ref ) {
				BaseRequest subReq = new BaseRequest( req, resolvedUri = ((Ref)source).getTargetUri() );
				source = TheGetter.getResponseValue( TheGetter.call( subReq ), subReq );
			}
			if( source instanceof Directory ) {
				Directory.Entry e = ((Directory)source).getDirectoryEntry(pathParts[i]);
				if( e == null ) {
					return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, "'" + pathParts[i] + "' not found in "+source.getClass().getName());
				}
				source = e.getTarget();
			} else if( source instanceof Commit ) {
				if( "target".equals(pathParts[i]) ) {
					source = ((Commit)source).getTarget();
				} else { 
					return new BaseResponse(ResponseCodes.RESPONSE_DOESNOTEXIST, "Cannot follow path " + path + " ('"+pathParts[i]+"' cannot be applied to a commit)");
				}
			} else {
				return new BaseResponse(ResponseCodes.RESPONSE_DOESNOTEXIST, "Cannot follow path " + path);
			}
		}
		if( source instanceof Ref ) {
			BaseRequest subReq = new BaseRequest( req, resolvedUri = ((Ref)source).getTargetUri() );
			source = TheGetter.getResponseValue( TheGetter.call( subReq ), subReq );
		}
		BaseResponse res = new BaseResponse(ResponseCodes.RESPONSE_NORMAL, source);
		if( resolvedUri != null ) {
			res.putMetadata(CCouchNamespace.RES_RESOLVED_URI, resolvedUri);
		}
		return res;
	}
	
	public Response call(Request req, Map argumentExpressions) {
		Object source = getArgumentValue(req, argumentExpressions, "source", null);
		if( source == null ) throw new RuntimeException("No source");
		String path = ValueUtil.getString(getArgumentValue(req, argumentExpressions, "path", null));
		if( path == null ) throw new RuntimeException("No path");
		
		return followPath( req, source, path );
	}
	
	//// Path simplification ////
	
	public Expression appendPath(Expression funcExpression, Map argumentExpressions, String path) {
		Expression sourceExpression = (Expression)argumentExpressions.get("source");
		if( sourceExpression == null ) throw new RuntimeException("No source expression");
		Expression pathExpression = (Expression)argumentExpressions.get("path");
		if( pathExpression == null ) throw new RuntimeException("No path expression");		
		if( !pathExpression.isConstant() ) return null;
		String firstPath = ValueUtil.getString(TheGetter.getResponseValue(pathExpression.eval(new BaseRequest()), pathExpression.toUri()));
		if( firstPath == null ) throw new RuntimeException("No path");
		String newPath = PathUtil.appendPath(firstPath, path, false);
		TreeMap newArgumentExpressions = new TreeMap();
		newArgumentExpressions.put("source", sourceExpression);
		newArgumentExpressions.put("path", new ValueExpression(newPath));
		return new FunctionCallExpression(funcExpression, newArgumentExpressions);
	}

	public Expression simplify(Map argumentExpressions) {
		Expression sourceExpression = (Expression)argumentExpressions.get("source");
		if( sourceExpression == null ) throw new RuntimeException("No source expression");
		Expression pathExpression = (Expression)argumentExpressions.get("path");
		
		sourceExpression = ActiveUtil.simplify(sourceExpression);
		if( !(sourceExpression instanceof PathSimplifiableExpression) ) return null;

		if( pathExpression == null ) throw new RuntimeException("No path expression");		
		if( !pathExpression.isConstant() ) return null;
		String path = ValueUtil.getString(TheGetter.getResponseValue(pathExpression.eval(new BaseRequest()), pathExpression.toUri()));
		if( path == null ) throw new RuntimeException("No path");

		return ((PathSimplifiableExpression)sourceExpression).appendPath(path);
	}
}
