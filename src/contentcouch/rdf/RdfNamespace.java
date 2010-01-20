package contentcouch.rdf;

public class RdfNamespace {
	public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	
	public static final String RDF_DESCRIPTION         = RDF_NS + "Description";

	public static final String RDF_ABOUT               = RDF_NS + "about";
	public static final String RDF_RESOURCE            = RDF_NS + "resource";
	public static final String RDF_PARSETYPE           = RDF_NS + "parseType";

	// Stand-in for actual value - may be 'class' but may be something else.
	// It's what rdf:Description is one of.
	public static final String RDF_CLASS               = RDF_NS + "class";
}
