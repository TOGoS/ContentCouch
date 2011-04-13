package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import togos.mf.value.Blob;
import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.ValueUtil;

public class SizeOf extends BaseActiveFunction {
	public Response call( Request req, Map argumentExpressions ) {
		Object o = getArgumentValue(req, argumentExpressions, "operand", null);
		if( o == null ) return new BaseResponse(ResponseCodes.DOES_NOT_EXIST,"No operand given to size-of");
		if( o instanceof Blob ) return new BaseResponse(ResponseCodes.NORMAL, new Long(((Blob)o).getLength()));
		if( o instanceof String ) return new BaseResponse(ResponseCodes.NORMAL, new Long(ValueUtil.getBytes((String)o).length));
		if( o instanceof byte[] ) return new BaseResponse(ResponseCodes.NORMAL, new Long(((byte[])o).length));
		throw new RuntimeException("Can't determine length of " + o.getClass().getName());
	}
}
