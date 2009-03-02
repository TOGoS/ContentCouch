package contentcouch.path;

public class PathUtil {
	public static String appendPath( String p1, String p2 ) {
		if( p1 == null || p1.length() == 0 ) return p2;
		if( p2 == null || p2.length() == 0 ) return p1;
		p1 = p1.replace('\\', '/');
		p2 = p2.replace('\\', '/');

		// If p2 is absolute, return it
		if( p2.matches("^(?:[a-zA-Z]:)?/.*") ) return p2;

		int lastSlash = p1.lastIndexOf('/');
		if( lastSlash == -1 ) return p2;

		return p1.substring(0,lastSlash+1) + p2;
	}
}
