package contentcouch.misc;

import java.util.Map;

import contentcouch.value.MetadataHaver;

public class MetadataUtil {
	public static Object getMetadataFrom(Object obj, String key) {
		if( !(obj instanceof MetadataHaver) ) return null;
		Map metadata = ((MetadataHaver)obj).getMetadata();
		if( metadata == null ) return null;
		return metadata.get(key);
	}
}
