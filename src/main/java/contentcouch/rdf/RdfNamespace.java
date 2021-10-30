package contentcouch.rdf;

public class RdfNamespace {
	public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	
	public static final String RDF_DESCRIPTION         = RDF_NS + "Description";

	public static final String RDF_ABOUT               = RDF_NS + "about";
	public static final String RDF_RESOURCE            = RDF_NS + "resource";
	public static final String RDF_PARSETYPE           = RDF_NS + "parseType";

	/**
	 * Value should be a ref.
	 * 
	 * e.g.
	 * <somens:Thing>
	 * </somens:Thing>
	 * 
	 * is equivalent to
	 * 
	 * <rdf:Description>
	 *   <rdf:type rdf:resource="http://somens.com/Thing"/>
	 * </rdf:Description>
	 */
	public static final String RDF_TYPE               = RDF_NS + "type";
}
