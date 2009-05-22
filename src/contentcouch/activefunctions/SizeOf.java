package contentcouch.activefunctions;

import java.util.Map;

import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.ValueUtil;
import contentcouch.value.Blob;

public class SizeOf extends BaseActiveFunction {
	public Object call( Map argumentExpressions ) {
		Object o = getArgumentValue(argumentExpressions, "operand", null);
		if( o == null ) return null;
		if( o instanceof Blob ) return new Long(((Blob)o).getLength());
		if( o instanceof String ) return new Long(ValueUtil.getBytes((String)o).length);
		if( o instanceof byte[] ) return new Long(((byte[])o).length);
		throw new RuntimeException("Can't determine length of " + o.getClass().getName());
	}
}
