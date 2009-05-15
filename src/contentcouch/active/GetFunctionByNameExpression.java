package contentcouch.active;

import java.util.Map;

import contentcouch.misc.UriUtil;

public class GetFunctionByNameExpression implements Expression {
	public static final String FUNCTION_MAP_VARNAME = "ccouch:functions";
	
	String funcName;
	
	public GetFunctionByNameExpression( String funcName ) {
		this.funcName = funcName;
	}
	
	public String toString() {
		return UriUtil.uriEncode(funcName);
	}
	
	public Object eval( Map context ) {
		Map functions = (Map)context.get(FUNCTION_MAP_VARNAME);
		if( functions == null ) throw new RuntimeException("No function map registered.");
		return functions.get(funcName);
	}
}
