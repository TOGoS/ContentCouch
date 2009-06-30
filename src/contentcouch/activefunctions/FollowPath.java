package contentcouch.activefunctions;

import java.util.Map;
import java.util.TreeMap;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.ActiveUtil;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.FunctionCallExpression;
import contentcouch.active.expression.Expression;
import contentcouch.active.expression.ValueExpression;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathSimplifiableActiveFunction;
import contentcouch.path.PathSimplifiableExpression;
import contentcouch.path.PathUtil;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class FollowPath extends BaseActiveFunction implements PathSimplifiableActiveFunction {
	public Response call(Map argumentExpressions) {
		Object source = getArgumentValue(argumentExpressions, "source", null);
		if( source == null ) throw new RuntimeException("No source");
		String path = ValueUtil.getString(getArgumentValue(argumentExpressions, "path", null));
		if( path == null ) throw new RuntimeException("No path");
		
		String[] pathParts = path.split("/+");
		for( int i=0; i<pathParts.length; ++i ) {
			if( source instanceof Ref ) {
				source = TheGetter.get( ((Ref)source).getTargetUri() );
			}
			if( source instanceof Directory ) {
				Directory.Entry e = ((Directory)source).getDirectoryEntry(pathParts[i]);
				if( e == null ) return new BaseResponse(Response.STATUS_NORMAL, pathParts[i] + " not found");
				source = e.getTarget();
			} else {
				return new BaseResponse(Response.STATUS_DOESNOTEXIST, "Cannot follow path " + path);
			}
		}
		if( source instanceof Ref ) {
			source = TheGetter.get( ((Ref)source).getTargetUri() );
		}
		return new BaseResponse(Response.STATUS_NORMAL, source);
	}
	
	//// Path simplification ////
	
	public Expression appendPath(Expression funcExpression, Map argumentExpressions, String path) {
		Expression sourceExpression = (Expression)argumentExpressions.get("source");
		if( sourceExpression == null ) throw new RuntimeException("No source expression");
		Expression pathExpression = (Expression)argumentExpressions.get("path");
		if( pathExpression == null ) throw new RuntimeException("No path expression");		
		if( !pathExpression.isConstant() ) return null;
		String firstPath = ValueUtil.getString(TheGetter.getResponseValue(pathExpression.eval(), pathExpression.toUri()));
		if( firstPath == null ) throw new RuntimeException("No path");
		String newPath = PathUtil.appendPath(firstPath, path);
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
		String path = ValueUtil.getString(TheGetter.getResponseValue(pathExpression.eval(), pathExpression.toUri()));
		if( path == null ) throw new RuntimeException("No path");

		return ((PathSimplifiableExpression)sourceExpression).appendPath(path);
	}
}
