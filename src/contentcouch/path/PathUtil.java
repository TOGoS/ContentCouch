package contentcouch.path;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import contentcouch.active.ActiveUtil;
import contentcouch.misc.UriUtil;


public class PathUtil {
	static Pattern URIPATTERN = Pattern.compile("^\\(.*|^\".*|^[^:/]+:.*", Pattern.DOTALL);
	
	public static boolean isUri( String pathOrUri ) {
		return URIPATTERN.matcher(pathOrUri).matches();
	}
	
	public static boolean isAbsolute( String pathOrUri ) {
		if( isUri(pathOrUri) ) return true;
		int slashi = pathOrUri.indexOf('/');
		int coloni = pathOrUri.indexOf(':');
		// This treats as absolute:
		//   /absolute/unix/filenames
		//   arbitrarily-schemed:uris/or/whatever
		//   F:/windows/paths
		return slashi == 0 || (coloni > 0 && (slashi > coloni || slashi < 0));
	}

	protected static Pattern SOMETHINGDOTDOT = Pattern.compile("(?:/[^/]+/\\.\\./)|(?:/\\./)", 0);
	protected static Pattern HIERARCHICAL = Pattern.compile("^(file:).*|^([^/:]+:)/.*");
	protected static Pattern AUTHORITY = Pattern.compile("([^/]+://[^/]*)/.*");
	
	public static boolean isHierarchicalUri( String uri ) {
		// File URIs and anything starting with "<scheme>:/"
		return HIERARCHICAL.matcher(uri).matches();
	}
	
	public static String appendHierarchicalPath( String p1, String p2, boolean ignoreLastInHierarchical ) {
		if( p2.startsWith("/") ) {
			if( !isUri(p1) ) return p2;
			Matcher authMatch = AUTHORITY.matcher(p1);
			if( authMatch.matches() ) {
				return authMatch.group(1) + p2;
			}
			
			Matcher hierMatch = HIERARCHICAL.matcher(p1);
			if( !hierMatch.matches() ) throw new RuntimeException("Looks like non-hierarchical source URI fed to appendHierarchicalPath: " + p1 );
			String pfx = hierMatch.group(1);
			if( pfx == null ) pfx = hierMatch.group(2);
			return pfx + p2;
		} else {
			if( isUri(p1) ) {
				// Strip off post-path parts
				int shedx = p1.indexOf('#');
				if( shedx > 0 ) p1 = p1.substring(0,shedx);
				int quedx = p1.indexOf('?');
				if( quedx > 0 ) p1 = p1.substring(0,quedx);
			}
			
			int lastSlash = p1.lastIndexOf('/');
			if( lastSlash == -1 ) return p2;
	
			if( ignoreLastInHierarchical ) {
				p1 = p1.substring(0,lastSlash+1);
			} else if( lastSlash != p1.length()-1 ) {
				p1 += "/";
			}
			
			String fp = p1 + p2;
			Matcher dotDotMatch = SOMETHINGDOTDOT.matcher(fp);
			while( dotDotMatch.find() ) {
				fp = dotDotMatch.replaceAll("/");
				dotDotMatch = SOMETHINGDOTDOT.matcher(fp);
			}
			return fp;
		}
	}
	
	public static String appendPath( String p1, String p2, boolean ignoreLastInHierarchical ) {
		if( p1 == null || p1.length() == 0 ) return p2;
		if( p2 == null || p2.length() == 0 ) return p1;
		p1 = p1.replace('\\', '/');
		p2 = p2.replace('\\', '/');

		// If p2 is absolute, return it
		if( isUri(p2) ) return p2;

		if( !isUri(p1) ) { // TODO: or if is hierarchical scheme to save some time (but should be taken care of below, anyway)
			// if p1 looks relative or a file path, we assume that it's hierarchical,
			// and we do simple old path appending.
			return appendHierarchicalPath(p1,p2,ignoreLastInHierarchical);
		}

		String unoptimized = "active:contentcouch.follow-path+" +
			"source@" + UriUtil.uriEncode(p1) + "+" +
			"path@" + UriUtil.uriEncode(UriUtil.makeDataUri(p2));
		return ActiveUtil.simplify(unoptimized);
	}
	
	public static String appendPath( String p1, String p2 ) {
		return appendPath( p1, p2, true );
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
		return UriUtil.uriDecode(path);
		/*
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
		*/
	}
	
	/** If the given string is not a URI, treat it as a file path and turn it into a file: URI */
	public static String maybeNormalizeFileUri( String uriOrPath ) {
		if( isUri(uriOrPath) ) return uriOrPath;
		if( uriOrPath.startsWith("//") ) return "file:" + uriEscapePath(uriOrPath);
		if( uriOrPath.startsWith("/") ) return "file://" + uriEscapePath(uriOrPath);
		if( uriOrPath.matches("^[A-Za-z]:.*") ) return "file:///" + uriEscapePath(uriOrPath); 
		return "file:" + uriEscapePath(uriOrPath);
	}
	
	public static class Path {
		public String path;
		
		public Path() {
			path = "NO PATH HERE WTF";
		}
		
		public Path( String path ) {
			this.path = path;
		}
		
		public boolean isAbsolute() {
			return path.startsWith("/") || path.matches("^[A-Za-z]:.*");
		}
		
		public String toString() {
			return path;
		}
	}
	
	public static class UncPath extends Path {
		public static String LOCALHOST = "localhost";
		
		public String host, path;
		
		public UncPath( String host, String path ) {
			this.host = host;
			this.path = path;
		}
		
		public boolean isAbsolute() {
			return true;
		}
		
		public boolean isLocal() {
			return LOCALHOST.equals(host);
		}
		
		public String toString() {
			return isLocal() ? path : ("//" + host + path);
		}
	}
	
	public static Path parseFilePathOrUri( String pathOrUri ) {
		if( pathOrUri.startsWith("file:") ) {
			return parseFilePathOrUri( uriUnescapePath(pathOrUri.substring(5)) );
		} else if( pathOrUri.startsWith("//") ) {
			String[] serverAndPath = pathOrUri.substring(2).split("/",2);
			return new UncPath( serverAndPath[0], "/" + (serverAndPath.length >= 2 ? serverAndPath[1] : ""));
		} else if( pathOrUri.matches("^[A-Za-z]:.*") ) {
			// Windows path!
			return new UncPath( UncPath.LOCALHOST, pathOrUri );
		} else if( pathOrUri.startsWith("/") ) {
			// Unix path!
			return new UncPath( UncPath.LOCALHOST, pathOrUri );
		} else {
			// Relative path!
			return new Path( pathOrUri );
		}
	}
}
