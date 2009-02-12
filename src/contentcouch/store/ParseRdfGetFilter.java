package contentcouch.store;

import contentcouch.blob.BlobUtil;
import contentcouch.rdf.RdfIO;
import contentcouch.rdf.RdfNamespace;
import contentcouch.rdf.RdfNode;
import contentcouch.value.Blob;

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
		} else if( identifier.startsWith(RdfNamespace.URI_PARSE_PREFIX) ) {
			parseIdentifier = identifier.substring(RdfNamespace.URI_PARSE_PREFIX.length());
		} else {
			parseIdentifier = null;
		}
		if( parseIdentifier != null ) {
			Object obj = get(parseIdentifier);
			if( obj == null ) return null;
			if( obj instanceof RdfNode ) return obj;
			Blob blob = BlobUtil.getBlob(obj);
			return RdfIO.parseRdf(BlobUtil.getString(blob), parseIdentifier);
		} else {
			return getter.get(identifier);
		}
	}

}
