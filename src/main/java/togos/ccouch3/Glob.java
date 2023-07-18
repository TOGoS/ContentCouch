package togos.ccouch3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Glob {
	public static final String VERSION = "1.0.0-2015.08.25";
	/** For purposes of pattern matching, always use this instead of File.separator */
	public static final String SEPARATOR = "/";
	
	protected final boolean negate;
	protected final Pattern pattern;
	protected final Glob next;
	protected Glob( boolean negate, Pattern p, Glob next ) {
		this.negate = negate;
		this.pattern = p;
		this.next = next;
	}
	
	protected static String path( File f ) {
		return f.getPath().replace(File.separator, SEPARATOR);
	}
	
	protected static Pattern ASTERSTICKS = Pattern.compile("/\\*\\*/|\\*\\*|\\*");
	
	protected static Glob parseGlobPattern( File relativeTo, String glob, Glob next ) {
		boolean negate;
		if( glob.startsWith("!") ) {
			negate = true;
			glob = glob.substring(1);
		} else {
			negate = false;
		}
		
		String regex;
		if( glob.startsWith("/") ) {
			if(relativeTo == null) throw new RuntimeException("Can't parse glob starting with '/' when relativeTo is null: "+glob);
			regex = "^"+Pattern.quote(path(relativeTo)+"/");
			glob = glob.substring(1);
		} else {
			regex = "(?:^|.*/)";
		}
		
		// Based on the * and ** rules used by .gitignore:
		//	https://git-scm.com/docs/gitignore
		
		if( glob.startsWith("**/") ) glob = glob.substring(3);
		
		Matcher starMatcher = ASTERSTICKS.matcher(glob);
		
		int i = 0;
		while( starMatcher.find() ) {
			regex += Pattern.quote(glob.substring(i,starMatcher.start()));
			if( "/**/".equals(starMatcher.group()) ) {
				regex += "(?:.*/)*";
			} else if( "**".equals(starMatcher.group()) ) {
				regex += ".*";
			} else if( "*".equals(starMatcher.group()) ) {
				regex += "[^/]*";
			} else {
				throw new RuntimeException("Weird match: "+starMatcher.group());
			}
			i = starMatcher.end();
		}
		regex += Pattern.quote(glob.substring(i));
		
		return new Glob(negate, Pattern.compile(regex), next);
	}
	
	public static Boolean anyMatch( Glob g, File f, Boolean defaultValue ) {
		String path = path(f);
		while( g != null ) {
			if( g.pattern.matcher(path).matches() ) {
				return Boolean.valueOf(!g.negate);
			}
			g = g.next;
		}
		return defaultValue;
	}
	
	public static boolean anyMatch( Glob g, File f ) {
		return anyMatch(g, f, Boolean.FALSE).booleanValue();
	}
	
	public static Glob load( File relativeTo, String[] lines, Glob next ) {
		for( int i=0; i<lines.length; ++i ) {
			String line = lines[i];
			line = line.trim();
			if( line.startsWith("#") || line.isEmpty() ) continue;
			next = Glob.parseGlobPattern(relativeTo, line, next);
		}
		return next;
	}
	
	public static Glob load( File f, Glob next ) throws IOException {
		File relativeTo = f.getParentFile();
		if(relativeTo == null) throw new RuntimeException("Couldn't determine relativeTo from glob file: "+f);
		
		FileReader fr = new FileReader(f);
		try {
			//@SuppressWarnings("resource")
			BufferedReader br = new BufferedReader(fr);
			String line;
			while( (line = br.readLine()) != null ) {
				line = line.trim();
				if( line.startsWith("#") || line.isEmpty() ) continue;
				next = parseGlobPattern(relativeTo, line, next);
			}
		} finally {
			fr.close();
		}
		return next;
	}
}
