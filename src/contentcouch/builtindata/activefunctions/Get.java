package contentcouch.builtindata.activefunctions;

import java.util.Map;

import togos.mf.api.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.builtindata.BuiltInData;
import contentcouch.misc.ValueUtil;

public class Get extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		return BuiltInData.getResponse(ValueUtil.getString(getArgumentValue(argumentExpressions, "operand", "")));
	}
}
