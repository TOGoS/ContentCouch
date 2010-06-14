package contentcouch.activefunctions;

import java.util.Iterator;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.base.BaseRequest;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.Expression;
import contentcouch.store.TheGetter;

public class Let extends BaseActiveFunction {

	public Response call(Request req, Map argumentExpressions) {
		BaseRequest subReq = new BaseRequest(req);
		for( Iterator i=argumentExpressions.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			String[] keyParts = ((String)e.getKey()).split("/",2);
			if( keyParts.length == 2 && "vars".equals(keyParts[0]) ) {
				Expression ex = (Expression)e.getValue();
				Object value = TheGetter.getResponseValue(ex.eval(req), ex.toUri());
				subReq.putMetadata( keyParts[1], value );
			}
		}
		return getArgumentResponse( subReq, argumentExpressions, "operand");
	}
}
