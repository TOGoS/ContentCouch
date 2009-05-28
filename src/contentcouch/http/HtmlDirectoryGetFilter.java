package contentcouch.http;

import contentcouch.directory.DirectoryUtil;
import contentcouch.store.Getter;
import contentcouch.value.Blob;

public class HtmlDirectoryGetFilter implements Getter {
	protected Getter parent;
	
	public HtmlDirectoryGetFilter(Getter parent) {
		this.parent = parent;
	}
	
	public Object get(String identifier) {
		Object o = parent.get(identifier);
		if( o instanceof Blob && identifier.endsWith("/") ) {
			return DirectoryUtil.parseHtmlDirectory( (Blob)o, identifier );
		}
		return o;
	}

}
