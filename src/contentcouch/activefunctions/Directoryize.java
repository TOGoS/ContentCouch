package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.Expression;
import contentcouch.active.expression.UriExpression;
import contentcouch.directory.DirectoryUtil;
import contentcouch.misc.UriUtil;
import contentcouch.value.Directory;

public class Directoryize extends BaseActiveFunction {
	public Response call(Request req, Map argumentExpressions) {
		Expression e = (Expression)argumentExpressions.get("operand");
		Response subRes = getArgumentResponse(req, argumentExpressions, "operand");
		if( subRes.getStatus() != ResponseCodes.NORMAL ) return subRes;
		if( e instanceof UriExpression ) {
			String uri = ((UriExpression)e).getTargetUri();
			if( uri.matches("^https?://.*/$") ) {
				Directory d = DirectoryUtil.getDirectory(subRes, "active:contentcouch.directoryize+operand@"+UriUtil.uriEncode(uri) );
				return new BaseResponse( ResponseCodes.NORMAL, d );
			}
		}
		return subRes;
	}

	//// Path simplification ////
	
	public String getPathArgumentName() {
		return "operand";
	}	
}
