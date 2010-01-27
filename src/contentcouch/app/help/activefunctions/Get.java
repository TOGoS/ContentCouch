package contentcouch.app.help.activefunctions;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.app.help.ContentCouchCommandHelp;
import contentcouch.misc.ValueUtil;

public class Get extends BaseActiveFunction {
	public Response call( Request req, Map argumentExpressions ) {
		return ContentCouchCommandHelp.getResponse(ValueUtil.getString(getArgumentValue(req, argumentExpressions, "operand", "")));
	}
}
