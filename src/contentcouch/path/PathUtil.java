package contentcouch.path;

import java.io.UnsupportedEncodingException;


public class PathUtil {
	public static boolean isAbsolute( String path ) {
		int slashi = path.indexOf('/');
		int coloni = path.indexOf(':');
		// This treats as absolute:
		//   /absolute/unix/filenames
		//   arbitrarily-schemed:uris/or/whatever
		//   F:/windows/paths
		return slashi == 0 || (coloni > 0 && (slashi > coloni || slashi < 0));
	}

	public static String appendPath( String p1, String p2 ) {
		if( p1 == null || p1.length() == 0 ) return p2;
		if( p2 == null || p2.length() == 0 ) return p1;
		p1 = p1.replace('\\', '/');
		p2 = p2.replace('\\', '/');

		// If p2 is absolute, return it
		if( isAbsolute(p2) ) return p2;

		int lastSlash = p1.lastIndexOf('/');
		if( lastSlash == -1 ) return p2;

		return p1.substring(0,lastSlash+1) + p2;
	}

	public static final char[] hexChars = new char[]{'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

	public static int hexDigit(char c) {
		if( c >= 'A' ) { 
			if( c >= 'a' ) return c - 'a' + 10;
			return c - 'A' + 10;
		}
		return c - '0';
	}
	
	public static String uriEscapePath( String path ) {
		byte[] bites;
		try {
			bites = path.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		StringBuffer b = new StringBuffer();
		for( int i=0; i<bites.length; ++i ) {
			char c = (char)bites[i];
			if( (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ) {
				b.append(c);
			} else {
				switch(c) {
				case('/'): case('+'): case('-'): case('.'): case(':'):
				case('~'): case('^'): case('('): case(')'): case('\\'):
				case('_'):
					b.append(c); break;
				default:
					b.append('%');
					b.append(hexChars[(c >> 4) & 0xF]);
					b.append(hexChars[(c >> 0) & 0xF]);
				}
			}
			    
		}
		return b.toString();
	}
	
	public static String uriUnescapePath( String path ) {
		byte[] bites = new byte[path.length()];
		int bi = 0;
		for( int ci=0; ci<path.length(); ++ci ) {
			char c = path.charAt(ci++);
			if( c == '%' ) {
				int d0 = hexDigit(path.charAt(ci++)) << 4;
				c = (char)(d0 + hexDigit(path.charAt(ci++)));
			}
			bites[bi++] = (byte)c;
		}
		try {
			return new String(bites, 0, bi, "UTF-8");
		} catch( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
	}
}
