package contentcouch.rdf;

import contentcouch.rdf.RdfIO.XMLEncodingContext;
import contentcouch.value.BaseRef;
import contentcouch.value.Ref;

public class RdfNode extends MultiMap
{
	private static final long serialVersionUID = 1L;
	
	public String subjectUri;
	public String sourceUri;
	
	public RdfNode() {
	}

	public RdfNode( RdfNode cloneFrom ) {
		super( cloneFrom );
		this.subjectUri = cloneFrom.subjectUri;
		this.sourceUri = cloneFrom.sourceUri;
	}

	public RdfNode(String typeName) {
		this.setRdfTypeUri( typeName );
	}

	public String getRdfTypeUri() {
		Ref typeRef = (Ref)getSingle(RdfNamespace.RDF_TYPE);
		return typeRef == null ? null : typeRef.getTargetUri();
	}
	
	public void setRdfTypeUri( String typeName ) {
		this.putSingle(RdfNamespace.RDF_TYPE, new BaseRef(typeName));
	}

	public String getSubjectUri() {
		return subjectUri;
	}
	
	@Deprecated
	public @Override String toString() {
		throw new RuntimeException("You probably want to use "+getClass()+"#toXml instead of #toString");
	}
	
	
	/**
	 * Returns a 'fill XML file' encoding this object.
	 * If you want to encode it as a piece of a bigger document,
	 * use RdfIO directly.
	 * 
	 * @return String full file of XML (with indentation and a trailing newline) encoding this object as RDF+XML
	 */
	public String toXml() {
		return RdfIO.xmlEncodeRdf(this, CCouchNamespace.CC_NS, XMLEncodingContext.FILE);
	}
}
