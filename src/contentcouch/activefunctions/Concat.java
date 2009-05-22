package contentcouch.activefunctions;

import java.util.Iterator;
import java.util.Map;

import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.ValueUtil;

public class Concat extends BaseActiveFunction {

	public Object call( Map argumentExpressions ) {
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

		return result;
	}
}
