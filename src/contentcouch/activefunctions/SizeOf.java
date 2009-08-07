package contentcouch.activefunctions;

import java.util.Map;

import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import togos.mf.value.Blob;
import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.ValueUtil;

public class SizeOf extends BaseActiveFunction {
	public Response call( Map argumentExpressions ) {
		Object o = getArgumentValue(argumentExpressions, "operand", null);
		if( o == null ) return new BaseResponse(ResponseCodes.RESPONSE_DOESNOTEXIST,"No operand given to size-of");
		if( o instanceof Blob ) return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, new Long(((Blob)o).getLength()));
		if( o instanceof String ) return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, new Long(ValueUtil.getBytes((String)o).length));
		if( o instanceof byte[] ) return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, new Long(((byte[])o).length));
		throw new RuntimeException("Can't determine length of " + o.getClass().getName());
	}
}
