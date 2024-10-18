package contentcouch.rdf;

import junit.framework.TestCase;

public class RdfEmitterTest extends TestCase
{
	public void testBlankNode() {
		RdfNode node = new RdfNode();
		assertEquals( "<rdf:Description xmlns=\"http://ns.nuke24.net/ContentCouch/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"/>", node.toXml());
	}
}
