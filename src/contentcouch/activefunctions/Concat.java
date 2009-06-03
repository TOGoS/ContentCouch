package contentcouch.activefunctions;

import java.util.Iterator;
import java.util.Map;

import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.ValueUtil;
import togos.rra.BaseResponse;
import togos.rra.Response;

public class Concat extends BaseActiveFunction {

	public Response call( Map argumentExpressions ) {
		String result = "";
		boolean prev = false;
		
		String separator = getArgumentValue( argumentExpressions, "separator", "" ).toString();
		
		for( Iterator i=getPositionalArgumentValues(argumentExpressions).iterator(); i.hasNext(); ) {
			Object v = i.next();
			if( v != null ) {
				if( prev ) result += separator;
				result += ValueUtil.getString(v);
				prev = true;
			}
		}

		return new BaseResponse(Response.STATUS_NORMAL, result);
	}
}
