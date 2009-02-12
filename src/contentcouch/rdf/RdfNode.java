/**
 * 
 */
package contentcouch.rdf;

public class RdfNode extends MultiMap {
	public String sourceUri;
	public String typeName;
	
	public RdfNode(String typeName) {
		this.typeName = typeName;
	}
	
	public String toString() {
		return RdfIO.xmlEncodeRdf(this, RdfNamespace.CCOUCH_NS);
	}
}