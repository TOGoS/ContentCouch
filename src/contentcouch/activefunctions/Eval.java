package contentcouch.activefunctions;

import java.util.Map;

import togos.rra.Getter;

import contentcouch.active.BaseActiveFunction;
import contentcouch.active.Context;
import contentcouch.misc.ValueUtil;

public class Eval extends BaseActiveFunction {
	public Object call( Map argumentExpressions ) {
		Object resolveThis = getArgumentValue(argumentExpressions, "operand", null);
		if( resolveThis == null ) return null;
		String uri = ValueUtil.getString(resolveThis);
		
		Getter uriResolverObj = (Getter)Context.getInstance().get(Context.GENERIC_GETTER_VAR);
		if( uriResolverObj == null ) {
			throw new RuntimeException("No ccouch:uri-resolver registered");
		}
		return uriResolverObj.get(uri);
	}
}
