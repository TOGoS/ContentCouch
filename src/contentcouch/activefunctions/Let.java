package contentcouch.activefunctions;

import java.util.Iterator;
import java.util.Map;

import togos.mf.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.Context;
import contentcouch.active.expression.Expression;
import contentcouch.store.TheGetter;

public class Let extends BaseActiveFunction {

	public Response call(Map argumentExpressions) {
		Context.pushNewDynamicScope();
		try {
			for( Iterator i=argumentExpressions.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry e = (Map.Entry)i.next();
				String[] keyParts = ((String)e.getKey()).split("/",2);
				if( keyParts.length == 2 && "vars".equals(keyParts[0]) ) {
					Expression ex = (Expression)e.getValue();
					Object value = TheGetter.getResponseValue(ex.eval(), ex.toUri());
					Context.put( keyParts[1], value );
				}
			}
			return getArgumentResponse(argumentExpressions, "operand");
		} finally {
			Context.popInstance();
		}
	}

}
