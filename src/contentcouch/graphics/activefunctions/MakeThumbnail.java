package contentcouch.graphics.activefunctions;

import java.util.Collections;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.store.TheGetter;

public class MakeThumbnail extends BaseActiveFunction {
	public Response call(Request req, Map argumentExpressions) {
		String id = TheGetter.identify(argumentExpressions.get("operand"), Collections.EMPTY_MAP);
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, "Thumbnail of "+id, "text/plain");
	}
}
