package contentcouch.misc;

import java.util.Iterator;
import java.util.Map;

import contentcouch.value.MetadataHaver;

public class MetadataUtil {
	public static Map getMetadataFrom( Object obj ) {
		if( !(obj instanceof MetadataHaver) ) return null;
		Map metadata = ((MetadataHaver)obj).getMetadata();
		if( metadata == null ) return null;
		return metadata;
	}
	
	public static Object getMetadataFrom( Object obj, String key ) {
		Map metadata = getMetadataFrom(obj);
		if( metadata == null ) return null;
		return metadata.get(key);
	}
	
	/** Copies any metadata found on src to dest.
	 * 
	 * @param dest object to copy metadata to
	 * @param src object to copy metadata from
	 * @return whether we are able to copy any metadata (even if there is none) to the target object
	 */
	public static boolean copyMetadata( Object dest, Object src ) {
		if( !(dest instanceof MetadataHaver) ) return false;
		final MetadataHaver mhDest = (MetadataHaver)dest;
		
		Map metadata = getMetadataFrom(src);
		if( metadata == null ) return true;
		
		for( Iterator mdi=metadata.entrySet().iterator(); mdi.hasNext(); ) {
			Map.Entry e = (Map.Entry)mdi.next();
			mhDest.putMetadata((String)e.getKey(), e.getValue());
		}
		return true;
	}
}
