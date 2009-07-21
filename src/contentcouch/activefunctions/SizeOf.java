package contentcouch.activefunctions;

import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.ValueUtil;
import contentcouch.value.Blob;

public class SizeOf extends BaseActiveFunction {
	public Response call( Map argumentExpressions ) {
		Object o = getArgumentValue(argumentExpressions, "operand", null);
		if( o == null ) return new BaseResponse(Response.STATUS_DOESNOTEXIST,"No operand given to size-of");
		if( o instanceof Blob ) return new BaseResponse(Response.STATUS_NORMAL, new Long(((Blob)o).getLength()));
		if( o instanceof String ) return new BaseResponse(Response.STATUS_NORMAL, new Long(ValueUtil.getBytes((String)o).length));
		if( o instanceof byte[] ) return new BaseResponse(Response.STATUS_NORMAL, new Long(((byte[])o).length));
		throw new RuntimeException("Can't determine length of " + o.getClass().getName());
	}
}
