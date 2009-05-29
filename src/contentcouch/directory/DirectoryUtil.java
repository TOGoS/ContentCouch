package contentcouch.directory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import contentcouch.blob.BlobInputStream;
import contentcouch.misc.SimpleDirectory;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.RdfIO;
import contentcouch.rdf.RdfNamespace;
import contentcouch.value.Blob;
import contentcouch.value.Directory;
import contentcouch.value.Ref;
import contentcouch.xml.XML;

public class DirectoryUtil {
	static Pattern HREF_PATTERN = Pattern.compile("(?:src|href|resource)=\"([^\\\"]*)\""); 
	
	public static Directory parseHtmlDirectory( Blob blob, String identifier ) {
		SimpleDirectory dir = new SimpleDirectory();
		BufferedReader r = new BufferedReader(new InputStreamReader(new BlobInputStream(blob)));
		String line;
		try {
			while( (line = r.readLine()) != null ) {
				Matcher lineMatcher = HREF_PATTERN.matcher(line);
				while( lineMatcher.find() ) {
					String href = lineMatcher.group(1);
					href = XML.xmlUnescape(href);
					String fullPath = PathUtil.appendPath(identifier, href);
					if( fullPath.startsWith(identifier) ) {
						String subPath = fullPath.substring(identifier.length());
						if( subPath.startsWith("?") ) continue;
						int si = subPath.indexOf('/');
						if( si == -1 ) {
							SimpleDirectory.Entry e = new SimpleDirectory.Entry();
							e.name = subPath;
							e.targetType = RdfNamespace.OBJECT_TYPE_BLOB;
							e.target = new Ref(PathUtil.appendPath(identifier, subPath));
							dir.addEntry(e);
						} else {
							SimpleDirectory.Entry e = new SimpleDirectory.Entry();
							e.name = subPath.substring(0, si);
							e.targetType = RdfNamespace.OBJECT_TYPE_DIRECTORY;
							e.target = new Ref(PathUtil.appendPath(identifier, e.name) + "/");
							dir.addEntry(e);
						}
					}
				}
			}
		} catch( IOException e ) {
			throw new RuntimeException("I/O error while reading HTML directory listing", e);
		}
		return dir;
	}
	
	public static Directory getDirectory( Object o, Map metadata, String identifier ) {
		if( o == null ) return null;
		if( o instanceof Directory ) {
			return (Directory)o;
		}
		if( o instanceof Blob ) {
			Blob blob = (Blob)o;
			String type = (String)metadata.get(RdfNamespace.DC_FORMAT);
			if( type == null || type.startsWith("text/html") ) {
				return parseHtmlDirectory(blob, identifier);
			}
			if( type.startsWith("application/rdf+xml") ) {
				return (Directory)RdfIO.parseRdf(ValueUtil.getString(blob), identifier);
			}
			return parseHtmlDirectory(blob, identifier);
		}
		throw new RuntimeException("Don't know how to turn " + o.getClass().getName() + " into Directory");	
	}
}
