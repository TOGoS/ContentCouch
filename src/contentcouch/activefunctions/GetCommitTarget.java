package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.framework.TheGetter;
import contentcouch.value.Commit;

public class GetCommitTarget extends BaseActiveFunction {
	public Response call(Request req, Map argumentExpressions) {
		Commit c = (Commit)getArgumentValue(req, argumentExpressions, "operand", null);
		return new BaseResponse(ResponseCodes.NORMAL, TheGetter.dereference(c.getTarget()));
	}
}
