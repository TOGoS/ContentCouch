package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.store.TheGetter;
import contentcouch.value.Commit;

public class GetCommitTarget extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		Commit c = (Commit)getArgumentValue(argumentExpressions, "operand", null);
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, TheGetter.dereference(c.getTarget()));
	}
}
