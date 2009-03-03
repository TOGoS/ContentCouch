package contentcouch.path;

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
}
