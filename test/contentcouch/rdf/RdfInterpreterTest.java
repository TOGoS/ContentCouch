package contentcouch.rdf;

import contentcouch.misc.ValueUtil;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;
import contentcouch.value.Ref;
import junit.framework.TestCase;

public class RdfInterpreterTest extends TestCase {
	static String OLDSTYLE_DIR_RDF_STRING =
		"<Directory xmlns=\"http://ns.nuke24.net/ContentCouch/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
		"<entries rdf:parseType=\"Collection\">" +
		"<DirectoryEntry>" +
		"<name>bob</name>" +
		"<targetType>Blob</targetType>" +
		"<target rdf:resource=\"data:,Hello,%20world!\"/>" +
		"</DirectoryEntry>" +
		"</entries>" +
		"</Directory>";
	
	static String NEWSTYLE_DIR_RDF_STRING =
		"<Directory xmlns=\"http://ns.nuke24.net/ContentCouch/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
		"<entries rdf:parseType=\"Collection\">" +
		"<DirectoryEntry>" +
		"<name>bob</name>" +
		"<target>" +
		"<Blob rdf:resource=\"data:,Hello,%20world!\"/>" +
		"<size>13</size>" +
		"</target>" +
		"</DirectoryEntry>" +
		"</entries>" +
		"</Directory>";

	public void setUp() {
		TheGetter.globalInstance = TheGetter.getBasicCallHandler();
	}
	
	protected void testParse( String parseThis ) {
		Object o = RdfIO.parseRdf(parseThis, getClass().getName());
		assertTrue( o instanceof RdfNode );
		Object sub = CCouchRdfInterpreter.getInstance().interpretSubject( (RdfNode)o );
		String subjectUri = ((RdfNode)o).getSubjectUri();
		assertNull( subjectUri );
		assertTrue( sub instanceof Directory );
		Directory dir = (Directory)sub;
		assertEquals( 1, dir.getDirectoryEntrySet().size() );
		Directory.Entry bob = dir.getDirectoryEntry("bob"); 
		assertNotNull( bob );
		assertEquals( CcouchNamespace.OBJECT_TYPE_BLOB, bob.getTargetType() );
		assertTrue( bob.getTarget() instanceof Ref );
		Ref bobTargetRef = (Ref)bob.getTarget();
		String rs = ValueUtil.getString( TheGetter.get( bobTargetRef.getTargetUri() ) );
		assertEquals( "Hello, world!", rs );
	}
	
	public void testParseOldStyle() {
		testParse( OLDSTYLE_DIR_RDF_STRING );
	}

	public void testParseNewStyle() {
		testParse( NEWSTYLE_DIR_RDF_STRING );
	}

	// TODO:
	// test parse new style
	// test interpreting x-parse-rdf:, x-rdf-subject: URIs
}
