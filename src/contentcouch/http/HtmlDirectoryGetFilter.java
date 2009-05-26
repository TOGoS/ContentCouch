package contentcouch.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import contentcouch.blob.BlobInputStream;
import contentcouch.misc.SimpleDirectory;
import contentcouch.path.PathUtil;
import contentcouch.rdf.RdfNamespace;
import contentcouch.store.Getter;
import contentcouch.value.Blob;
import contentcouch.value.Ref;
import contentcouch.xml.XML;

public class HtmlDirectoryGetFilter implements Getter {
	protected Getter parent;
	
	public HtmlDirectoryGetFilter(Getter parent) {
		this.parent = parent;
	}
	
	static Pattern HREF_PATTERN = Pattern.compile("(?:src|href|resource)=\"([^\\\"]*)\""); 
	
	public Object get(String identifier) {
		Object o = parent.get(identifier);
		if( o instanceof Blob && identifier.endsWith("/") ) {
			SimpleDirectory dir = new SimpleDirectory();
			BufferedReader r = new BufferedReader(new InputStreamReader(new BlobInputStream((Blob)o)));
			String line;
			try {
				while( (line = r.readLine()) != null ) {
					Matcher lineMatcher = HREF_PATTERN.matcher(line);
					while( lineMatcher.find() ) {
						String href = lineMatcher.group(1);
						href = XML.xmlUnescape(href);
						String fullPath = PathUtil.appendPath(identifier, href);
						System.err.println(fullPath);
						if( fullPath.startsWith(identifier) ) {
							String subPath = fullPath.substring(identifier.length());
							if( subPath.startsWith("?") ) continue;
							int si = subPath.indexOf('/');
							if( si == -1 ) {
								SimpleDirectory.Entry e = new SimpleDirectory.Entry();
								e.name = subPath;
								e.targetType = RdfNamespace.OBJECT_TYPE_BLOB;
								e.target = new Ref(subPath);
								dir.addEntry(e);
							} else {
								SimpleDirectory.Entry e = new SimpleDirectory.Entry();
								e.name = subPath.substring(0, si+1);
								e.targetType = RdfNamespace.OBJECT_TYPE_DIRECTORY;
								e.target = new Ref(e.name);
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
		return o;
	}

}
