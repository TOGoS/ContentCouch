package contentcouch.rdf;

import java.text.ParseException;

import contentcouch.date.DateUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;
import contentcouch.value.Ref;
import junit.framework.TestCase;

public class RdfInterpreterTest extends TestCase {
	static String OLDSTYLE_DIR_RDF_STRING =
		"<Directory xmlns=\"http://ns.nuke24.net/ContentCouch/\" " +
		"    xmlns:dcterms=\"http://purl.org/dc/terms/\"\n" +
		"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
		"  <entries rdf:parseType=\"Collection\">\n" +
		"    <DirectoryEntry>\n" +
		"      <name>bob</name>\n" +
		"      <dcterms:modified>2010-01-22 15:20:35 GMT</dcterms:modified>\n" +
		"      <size>13</size>\n" +
		"      <target rdf:resource=\"data:,Hello,%20world!\"/>\n" +
		"      <targetType>Blob</targetType>\n" +
		"    </DirectoryEntry>\n" +
		"  </entries>\n" +
		"</Directory>";
	
	static String NEWSTYLE_DIR_RDF_STRING =
		"<Directory xmlns=\"http://ns.nuke24.net/ContentCouch/\"\n" +
		"    xmlns:bz=\"http://bitzi.com/xmlns/2002/01/bz-core#\"\n" +
		"    xmlns:dcterms=\"http://purl.org/dc/terms/\"\n" +
		"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
		"  <entries rdf:parseType=\"Collection\">\n" +
		"    <DirectoryEntry>\n" +
		"      <name>bob</name>\n" +
		"      <dcterms:modified>2010-01-22 15:20:35 GMT</dcterms:modified>\n" +
		"      <target>\n" +
		"        <Blob rdf:about=\"data:,Hello,%20world!\">\n" +
		"          <bz:fileLength>13</bz:fileLength>\n" +
		"        </Blob>\n" +
		"      </target>\n" +
		"    </DirectoryEntry>\n" +
		"  </entries>\n" +
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
		assertEquals( CCouchNamespace.TT_SHORTHAND_BLOB, bob.getTargetType() );
		assertTrue( bob.getTarget() instanceof Ref );
		assertEquals( 13l, bob.getTargetSize() );
		try {
			assertEquals( DateUtil.parseDate("2010-01-22 15:20:35 GMT").getTime(), bob.getLastModified() );
		} catch( ParseException e ) {
			throw new RuntimeException(e);
		}
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
