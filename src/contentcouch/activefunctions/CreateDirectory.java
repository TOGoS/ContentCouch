package contentcouch.activefunctions;

import java.util.Iterator;
import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.Expression;
import contentcouch.misc.SimpleDirectory;
import contentcouch.misc.ValueUtil;
import contentcouch.store.TheGetter;
import contentcouch.value.BaseRef;

public class CreateDirectory extends BaseActiveFunction {

	public Response call(Map argumentExpressions) {
		SimpleDirectory result = new SimpleDirectory();
		
		boolean lazy = ValueUtil.getBoolean(getArgumentValue(argumentExpressions, "lazy", null), false);
		
		for( Iterator i=argumentExpressions.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			String key = (String)e.getKey();
			System.err.println("Look at arg " + key);
			final String[] keyParts = key.split("/");
			if( keyParts.length == 2 && "entries".equals(keyParts[0]) ) {
				final String entryKey = keyParts[1];
				System.err.println("Entry: " + entryKey);
				Expression targetExpression = (Expression)e.getValue();
				if( lazy ) {
					SimpleDirectory.Entry newEntry = new SimpleDirectory.Entry();
					newEntry.target = new BaseRef(targetExpression.toUri());
					newEntry.name = entryKey;
					result.addDirectoryEntry(newEntry);
				} else {
					result.put( entryKey, TheGetter.getResponseValue( targetExpression.eval(), targetExpression.toUri() ) );
				}
			}
		}
		
		return new BaseResponse(Response.STATUS_NORMAL, result);
	}

}
