package contentcouch.rdf;

import contentcouch.value.BaseRef;
import contentcouch.value.Ref;

public class RdfNode extends MultiMap {
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
	
	public String toString() {
		return RdfIO.xmlEncodeRdf(this, CcouchNamespace.CC_NS);
	}
}