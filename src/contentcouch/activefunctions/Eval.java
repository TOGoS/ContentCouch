package contentcouch.activefunctions;

import java.util.Map;

import togos.rra.BaseRequest;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.ValueUtil;
import contentcouch.store.TheGetter;

public class Eval extends BaseActiveFunction {
	public Response call( Map argumentExpressions ) {
		Object resolveThis = getArgumentValue(argumentExpressions, "operand", null);
		if( resolveThis == null ) return null;
		String uri = ValueUtil.getString(resolveThis);
		
		return TheGetter.handleRequest(new BaseRequest(BaseRequest.VERB_GET, uri));
	}
}
