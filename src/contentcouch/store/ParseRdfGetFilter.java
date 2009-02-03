package contentcouch.store;

import contentcouch.data.Blob;
import contentcouch.data.BlobUtil;
import contentcouch.xml.RDF;

public class ParseRdfGetFilter implements Getter {
	Getter getter;
	public boolean handleAtSignAsParseRdf = false;
	
	public ParseRdfGetFilter(Getter getter) {
		this.getter = getter;
	}
	
	public Object get(String identifier) {
		String parseIdentifier;
		if( handleAtSignAsParseRdf && identifier.charAt(0) == '@' ) {
			parseIdentifier = identifier.substring(1);
		} else if( identifier.startsWith(RDF.URI_PARSE_PREFIX) ) {
			parseIdentifier = identifier.substring(RDF.URI_PARSE_PREFIX.length());
		} else {
			parseIdentifier = null;
		}
		if( parseIdentifier != null ) {
			Object obj = get(parseIdentifier);
			if( obj instanceof RDF.RdfNode ) {
				return obj;
			}
			Blob blob = BlobUtil.getBlob(obj);
			return RDF.parseRdf(BlobUtil.getString(blob), parseIdentifier);
		} else {
			return getter.get(identifier);
		}
	}

}
