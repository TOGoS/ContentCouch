package contentcouch.activefunctions;

import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.Expression;
import contentcouch.active.ResolveUriExpression;
import contentcouch.directory.DirectoryUtil;
import contentcouch.value.Directory;

public class Directoryize extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		Expression e = (Expression)argumentExpressions.get("operand");
		Response subRes = getArgumentResponse(argumentExpressions, "operand");
		if( e instanceof ResolveUriExpression ) {
			String uri = ((ResolveUriExpression)e).getUri();
			if( uri.matches("^https?://.*/$") ) {
				Directory d = DirectoryUtil.getDirectory(subRes, uri);
				return new BaseResponse( Response.STATUS_NORMAL, d );
			}
		}
		return subRes;
	}
}