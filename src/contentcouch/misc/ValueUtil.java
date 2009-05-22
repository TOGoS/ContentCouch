package contentcouch.misc;

import java.io.UnsupportedEncodingException;

import contentcouch.blob.BlobUtil;
import contentcouch.value.Blob;

public class ValueUtil {
	//// Get bytes ////
	
	public static byte[] getBytes(String s) {
		try {
			return s.getBytes("UTF-8");
		} catch( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
	}
	
	//// Get strings ////
	
	public static String getString(byte[] bytes) {
		try {
			return new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String getString(Object obj) {
		if( obj == null ) return null;
		if( obj instanceof String ) return (String)obj;
		if( obj instanceof byte[] ) return getString((byte[])obj);
		if( obj instanceof Blob ) return BlobUtil.getString((Blob)obj);
		return obj.toString();
	}
	
	//// Get numbers ////
	
	public static Number getNumber( Object o, Number defaultValue ) {
		if( o == null ) {
			return defaultValue;
		} else if( o instanceof Number ) {
			return (Number)o;
		} else {
			return Double.valueOf(getString(o));
		}
	}
	
	public static Number getNumber( Object o ) {
		return getNumber( o, null );
	}
}
