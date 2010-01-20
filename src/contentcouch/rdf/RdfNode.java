package contentcouch.rdf;

public class RdfNode extends MultiMap {
	public String subjectUri;
	public String sourceUri;
	
	public RdfNode(String typeName) {
		this.setRdfClassName( typeName );
	}

	public String getRdfClassName() {
		return (String)getSingle(RdfNamespace.RDF_CLASS);
	}
	
	public void setRdfClassName( String typeName ) {
		this.putSingle(RdfNamespace.RDF_CLASS, typeName);
	}

	public String getSubjectUri() {
		return subjectUri;
	}
	
	public String toString() {
		return RdfIO.xmlEncodeRdf(this, CcouchNamespace.CC_NS);
	}
}