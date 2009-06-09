package contentcouch.explorify;

/** Processes URIs to make them work within whatever framework is being used to generate web pages.
 * This should not be used to process URIs passed around internally. */
public interface UriProcessor {
	public String processUri( String uri );
	public String processRelativeUri( String baseUri, String relativeUri );
}
