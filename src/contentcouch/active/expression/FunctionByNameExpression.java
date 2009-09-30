package contentcouch.active.expression;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.misc.UriUtil;

public class FunctionByNameExpression implements Expression {
	public static final String FUNCTION_MAP_VARNAME = "ccouch:functions";
	
	String funcName;
	
	public FunctionByNameExpression( String funcName ) {
		this.funcName = funcName;
	}
	
	public String toString() {
		return UriUtil.uriEncode(funcName);
	}
	
	public String toUri() {
		return "x-activefunction:" + UriUtil.uriEncode(this.funcName);
	}
	
	protected static final Class getClassByNameIfExists(String name) {
		try {
			return Class.forName(name);
		} catch( NoClassDefFoundError e ) {
			return null;
		} catch( ClassNotFoundException e ) {
			return null;
		}
	}
	
	protected static final char upChar( char c ) {
		if( c >= 'a' && c <= 'z' ) return (char)(c + 'A' - 'a');
		return c;
	}
	
	protected static final String dashedToCamelCase(String dashed, boolean upcaseFirst) {
		char[] dashedChars = dashed.toCharArray();
		int dashCount = 0;
		for( int i=0; i<dashedChars.length; ++i ) {
			if( dashedChars[i] == '-' ) ++dashCount;
		}
		char[] camelCaseChars = new char[dashedChars.length-dashCount];
		boolean nextCharUpper = upcaseFirst;
		for( int i=0,j=0; i<dashedChars.length; ++i ) {
			if( dashedChars[i] == '-' ) {
				nextCharUpper = true;
			} else {
				camelCaseChars[j++] = nextCharUpper ? upChar(dashedChars[i]) : dashedChars[i];
				nextCharUpper = false;
			}
		}
		return new String(camelCaseChars);
	}
	
	protected static final String getClassNameForFunction( String funcName ) {
		String[] modParts = funcName.split("\\.");
		String[] mungeModParts = new String[modParts.length+1];
		for( int i=0; i<modParts.length-1; ++i ) {
			mungeModParts[i] = modParts[i];
		}
		mungeModParts[modParts.length-1] = "activefunctions";
		mungeModParts[modParts.length] = dashedToCamelCase(modParts[modParts.length-1], true);
		String className = mungeModParts[0];
		for( int i=1; i<mungeModParts.length; ++i ) {
			className += "." + mungeModParts[i];
		}
		return className;
	}
	
	protected static final Class getClassForFunction( String funcName ) {
		String className = getClassNameForFunction( funcName );
		return getClassByNameIfExists( className );
	}
	
	public Response eval(Request req) {
		Map functions = (Map)req.getContextVars().get(FUNCTION_MAP_VARNAME);

		// Check function map
		if( functions != null ) {
			Object f = functions.get(funcName);
			if( f != null ) return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, f);
		}
		
		Class c = getClassForFunction( funcName );
		if( c != null ) {
			try {
				return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, c.newInstance());
			} catch( InstantiationException e ) {
				throw new RuntimeException(e);
			} catch( IllegalAccessException e ) {
				throw new RuntimeException(e);
			}
		}
		
		return new BaseResponse(ResponseCodes.RESPONSE_DOESNOTEXIST, "Couldn't find function " + funcName, "text/plain");
	}

	public boolean isConstant() {
		return true;
	}
}
