package contentcouch.misc;

import java.util.Map;

public class MapUtil {
	public static Object getKeyed( Map m, String key, Object defaultValue ) {
		if( m == null ) return defaultValue;
		Object value = m.get(key);
		if( value == null ) return defaultValue;
		return value;
	}
	
	public static Object getKeyed( Map m, String key ) {
		return getKeyed( m, key, null );
	}
}
