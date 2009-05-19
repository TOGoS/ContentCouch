package contentcouch.activefunctions;

import java.util.Iterator;
import java.util.Map;

import contentcouch.active.BaseActiveFunction;
import contentcouch.active.Expression;
import contentcouch.misc.ValueUtil;

public class Concat extends BaseActiveFunction {

	public Object call(Map context, Map argumentExpressions) {
		String result = "";
		boolean prev = false;
		
		String separator = getArgumentValue( context, argumentExpressions, "separator", "" ).toString();
		
		for( Iterator i=argumentExpressions.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			String key = (String)e.getKey();
			if( key.startsWith("operand") ) {
				if( prev ) result += separator;
				result += ValueUtil.getString(((Expression)e.getValue()).eval(context));
				prev = true;
			}
			
		}
		return result;
	}
}
