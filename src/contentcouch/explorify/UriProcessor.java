package contentcouch.explorify;

/** Processes URIs to make them work within whatever framework is being used to generate web pages.
 * This should not be used to process URIs passed around internally. */
public interface UriProcessor {
	public String processUri( String uri );
	/** This method should detect absolute URIs and pass them through to processUri
	 * rather than assuming all passed in URIs are actually relative */
	public String processRelativeUri( String baseUri, String relativeUri );
}
