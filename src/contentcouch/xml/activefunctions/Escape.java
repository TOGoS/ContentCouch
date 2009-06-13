package contentcouch.xml.activefunctions;

import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.ValueUtil;
import contentcouch.xml.XML;

public class Escape extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		String text = ValueUtil.getString(getArgumentValue(argumentExpressions, "operand", ""));
		return new BaseResponse(Response.STATUS_NORMAL, XML.xmlEscapeText(text), "text/plain");
	}
}
