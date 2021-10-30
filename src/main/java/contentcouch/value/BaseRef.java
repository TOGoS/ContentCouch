package contentcouch.value;

import contentcouch.path.PathUtil;

public class BaseRef implements RelativeRef {
	String absoluteUri;
	String baseUri;
	String relativeUri;
	
	public BaseRef( String absoluteUri ) {
		this.absoluteUri = absoluteUri;
	}
	
	public BaseRef( String baseUri, String relativeUri ) {
		this.baseUri = baseUri;
		this.relativeUri = relativeUri;
	}

	/** @param baseUri
	 * @param relativeUri
	 * @param absoluteUri may be specified as a more direct way to get at the resource
	 * than PathUtil.appendPath( baseUri, relativeUri )
	 */ 
	public BaseRef( String baseUri, String relativeUri, String absoluteUri ) {
		this.baseUri = baseUri;
		this.relativeUri = relativeUri;
		this.absoluteUri = absoluteUri;
	}

	public boolean isRelative() {
		return relativeUri != null && PathUtil.isRelative(relativeUri);
	}
	
	public String getTargetUri() {
		// Cache baseUri + relativeUri in absoluteUri and return it
		if( absoluteUri == null && relativeUri != null ) {
			absoluteUri = PathUtil.appendPath(baseUri, relativeUri);
		}
		return absoluteUri;
	}

	public String getTargetBaseUri() {
		return baseUri;
	}
	public String getTargetRelativeUri() {
		return relativeUri;
	}
}
