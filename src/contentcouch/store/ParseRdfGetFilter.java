package contentcouch.store;

import contentcouch.blob.BlobUtil;
import contentcouch.rdf.RdfIO;
import contentcouch.rdf.RdfNamespace;
import contentcouch.rdf.RdfNode;
import contentcouch.value.Blob;

public class ParseRdfGetFilter implements Getter {
	Getter getter;
	public boolean passThroughOtherUris;
	public boolean handleAtSignAsParseRdf = false;
	
	public ParseRdfGetFilter(Getter getter) {
		this(getter,true);
	}

	public ParseRdfGetFilter(Getter getter, boolean passThroughOtherUris) {
		this.getter = getter;
		this.passThroughOtherUris = passThroughOtherUris;
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
		} else if( passThroughOtherUris ) {
			return getter.get(identifier);
		} else {
			return null;
		}
	}

}
