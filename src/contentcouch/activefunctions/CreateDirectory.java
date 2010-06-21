package contentcouch.activefunctions;

import java.util.Iterator;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.Expression;
import contentcouch.directory.SimpleDirectory;
import contentcouch.misc.ValueUtil;
import contentcouch.store.TheGetter;
import contentcouch.value.BaseRef;

public class CreateDirectory extends BaseActiveFunction {

	public Response call(Request req, Map argumentExpressions) {
		SimpleDirectory result = new SimpleDirectory();
		
		boolean lazy = ValueUtil.getBoolean(getArgumentValue(req, argumentExpressions, "lazy", null), false);
		
		for( Iterator i=argumentExpressions.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			String key = (String)e.getKey();
			final String[] keyParts = key.split("/");
			if( keyParts.length == 2 && "entries".equals(keyParts[0]) ) {
				final String entryKey = keyParts[1];
				Expression targetExpression = (Expression)e.getValue();
				if( lazy ) {
					SimpleDirectory.Entry newEntry = new SimpleDirectory.Entry();
					newEntry.target = new BaseRef(targetExpression.toUri());
					newEntry.name = entryKey;
					result.addDirectoryEntry(newEntry, req.getMetadata());
				} else {
					result.put( entryKey, TheGetter.getResponseValue( targetExpression.eval(req), targetExpression.toUri() ) );
				}
			}
		}
		
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, result);
	}

}
