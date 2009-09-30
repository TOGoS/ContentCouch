package togos.swf2;

import java.util.Map;


public class ConfigUtil {
	public static Object getConfigValueFromContext( Map context, String key, Object defaultValue ) {
		Map config = (Map)context.get(SwfNamespace.CTX_CONFIG);
		if( config == null ) return defaultValue;
		Object value = config.get(key);
		if( value == null ) return defaultValue;
		return value;
	}
}
