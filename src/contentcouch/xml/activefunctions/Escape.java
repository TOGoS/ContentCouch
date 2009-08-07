package contentcouch.xml.activefunctions;

import java.util.Map;

import togos.mf.ResponseCodes;
import togos.mf.Response;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.ValueUtil;
import contentcouch.xml.XML;

public class Escape extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		String text = ValueUtil.getString(getArgumentValue(argumentExpressions, "operand", ""));
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, XML.xmlEscapeText(text), "text/plain");
	}
}
