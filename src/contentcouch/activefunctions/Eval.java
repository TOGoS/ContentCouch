package contentcouch.activefunctions;

import java.util.Map;

import contentcouch.active.BaseActiveFunction;
import contentcouch.active.Context;
import contentcouch.misc.ValueUtil;
import contentcouch.store.Getter;

public class Eval extends BaseActiveFunction {
	public Object call( Map argumentExpressions ) {
		Object resolveThis = getArgumentValue(argumentExpressions, "operand", null);
		if( resolveThis == null ) return null;
		String uri = ValueUtil.getString(resolveThis);
		
		Getter uriResolverObj = (Getter)Context.getInstance().get(Context.URI_RESOLVER_VARNAME);
		if( uriResolverObj == null ) {
			throw new RuntimeException("No ccouch:uri-resolver registered");
		}
		return uriResolverObj.get(uri);
	}
}
