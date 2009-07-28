package contentcouch.rdf;

import contentcouch.value.Ref;

public class Description extends RdfNode {
	public Ref about;
	public Description() {
		super(RdfNamespace.RDF_DESCRIPTION);
	}
}