package contentcouch.activefunctions;

import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.Expression;
import contentcouch.active.expression.ResolveUriExpression;
import contentcouch.directory.DirectoryUtil;
import contentcouch.misc.UriUtil;
import contentcouch.value.Directory;

public class Directoryize extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		Expression e = (Expression)argumentExpressions.get("operand");
		Response subRes = getArgumentResponse(argumentExpressions, "operand");
		if( e instanceof ResolveUriExpression ) {
			String uri = ((ResolveUriExpression)e).getUri();
			if( uri.matches("^https?://.*/$") ) {
				Directory d = DirectoryUtil.getDirectory(subRes, "active:contentcouch.directoryize+operand@"+UriUtil.uriEncode(uri) );
				return new BaseResponse( Response.STATUS_NORMAL, d );
			}
		}
		return subRes;
	}

	//// Path simplification ////
	
	public String getPathArgumentName() {
		return "operand";
	}	
}
