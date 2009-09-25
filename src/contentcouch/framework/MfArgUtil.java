package contentcouch.framework;

import java.util.HashMap;
import java.util.Map;

import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.value.BaseRef;
import togos.mf.api.Request;
import togos.mf.base.BaseArguments;
import togos.mf.base.BaseRequest;
import togos.mf.value.Arguments;

public class MfArgUtil {
	
	public static Arguments addArguments( Object content, Map newArgumentValues ) {
		BaseArguments newArguments;
		if( content == null ) {
			newArguments = new BaseArguments(null, newArgumentValues);
		} else if( content instanceof Arguments ) {
			Map argVals = new HashMap();
			argVals.putAll(((Arguments) content).getNamedArguments());
			argVals.putAll(newArgumentValues);
			 newArguments = new BaseArguments(((Arguments) content).getPositionalArguments(), argVals);
		} else {
			newArguments = new BaseArguments(null, newArgumentValues);
			newArguments.addPositionalArgument(content);
		}
		return newArguments;
	}
	
	public static Request addArguments( Request input, Map newArgumentValues, String newResourceName ) {
		BaseRequest nr = new BaseRequest(input, newResourceName);
		Object content = nr.content;
		Arguments newArgs = addArguments( content, newArgumentValues );
		nr.content = newArgs;
		return nr;
	}
	
	/** Alters any request with a URI of the form ?a=b&c=d...
	 *  by putting those arguments into the request content.
	 * @param input
	 * @return the original request if no changes are needed,
	 *   otherwise a copy with altered resourceName and content
	 */
	public static Request argumentizeQueryString( Request input ) {
		String rn = input.getResourceName();
		int qss = rn.indexOf('?');
		if( qss == -1 ) return input;
		
		String queryString = rn.substring(qss+1);
		if( queryString.length() == 0 ) return input;
		
		Map newArgs = new HashMap();
		String[] parts = queryString.split("&");
		for( int i=0; i<parts.length; ++i ) {
			String part = parts[i];
			int eqi = part.indexOf('=');
			if( eqi == -1 ) {
				String value = UriUtil.uriDecode(part);
				newArgs.put(value,value);
			} else {
				String key = UriUtil.uriDecode(part.substring(0,eqi));
				String value = UriUtil.uriDecode(part.substring(eqi+1));
				newArgs.put(key,value);
			}
		}
		return addArguments( input, newArgs, rn.substring(0,qss) );
	}
	
	public static Object getPrimaryArgument( Object content ) {
		if( content instanceof Arguments ) {
			Arguments args = (Arguments)content;
			if( args.getPositionalArguments().size() > 0 ) {
				return args.getPositionalArguments().get(0);
			}
			Object operand = args.getNamedArguments().get("operand");
			if( operand != null ) return operand;
			String operandUri = ValueUtil.getString(args.getNamedArguments().get("operandUri"));
			if( operandUri != null ) return new BaseRef(operandUri);
			return null;
		} else {
			return content;
		}
	}
}
