package contentcouch.activefunctions;

import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.store.TheGetter;
import contentcouch.value.Commit;

public class GetCommitTarget extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		Commit c = (Commit)getArgumentValue(argumentExpressions, "operand", null);
		return new BaseResponse(Response.STATUS_NORMAL, TheGetter.dereference(c.getTarget()));
	}
}
