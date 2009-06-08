package contentcouch.directory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import togos.rra.ContentAndMetadata;

import contentcouch.blob.BlobInputStream;
import contentcouch.misc.SimpleDirectory;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.rdf.DcNamespace;
import contentcouch.rdf.RdfIO;
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
					if( href.startsWith("./") ) href = href.substring(2);
					if( href.startsWith("?") ) continue;
					if( PathUtil.isAbsolute(href) ) continue;
					int si = href.indexOf('/');
					if( si != -1 ) href = href.substring(0,si+1);
					if( !href.endsWith("/") ) {
						SimpleDirectory.Entry e = new SimpleDirectory.Entry();
						e.name = href;
						e.targetType = CcouchNamespace.OBJECT_TYPE_BLOB;
						e.target = new Ref(PathUtil.appendPath(identifier, href));
						dir.addEntry(e);
					} else {
						SimpleDirectory.Entry e = new SimpleDirectory.Entry();
						e.name = href.substring(0, href.length()-1);
						e.targetType = CcouchNamespace.OBJECT_TYPE_DIRECTORY;
						e.target = new Ref(PathUtil.appendPath(identifier, e.name) + "/");
						dir.addEntry(e);
					}
				}
			}
		} catch( IOException e ) {
			throw new RuntimeException("I/O error while reading HTML directory listing", e);
		}
		return dir;
	}
	
	public static Directory getDirectory( Object o, Map metadata, String uri ) {
		if( o == null ) return null;
		if( o instanceof Directory ) {
			return (Directory)o;
		}
		if( o instanceof Blob ) {
			Blob blob = (Blob)o;
			String type = (String)metadata.get(DcNamespace.DC_FORMAT);
			if( type == null || type.startsWith("text/html") ) {
				return parseHtmlDirectory(blob, uri);
			}
			if( type.startsWith("application/rdf+xml") ) {
				return (Directory)RdfIO.parseRdf(ValueUtil.getString(blob), uri);
			}
			return parseHtmlDirectory(blob, uri);
		}
		throw new RuntimeException("Don't know how to turn " + o.getClass().getName() + " into Directory");	
	}
	
	public static Directory getDirectory( ContentAndMetadata res, String uri ) {
		return getDirectory(res.getContent(), res.getContentMetadata(), uri);
	}
}
