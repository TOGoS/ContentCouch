package contentcouch.activefunctions;

import java.util.Iterator;
import java.util.Map;

import contentcouch.active.ActiveFunction;
import contentcouch.active.Expression;
import contentcouch.blob.BlobUtil;

public class Concat implements ActiveFunction {

	public Object call(Map context, Map argumentExpressions) {
		String result = "";
		boolean prev = false;
		Expression separatorExpression = (Expression)argumentExpressions.get("separator");		
		String separator = (separatorExpression != null) ? separatorExpression.eval(context).toString() : "";
		for( Iterator i=argumentExpressions.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			String key = (String)e.getKey();
			if( key.startsWith("operand") ) {
				if( prev ) result += separator;
				result += BlobUtil.getString(((Expression)e.getValue()).eval(context));
				prev = true;
			}
			
		}
		return result;
	}
}
