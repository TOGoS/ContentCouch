package contentcouch.directory;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import contentcouch.context.Context;
import contentcouch.date.DateUtil;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.rdf.RdfDirectory;
import contentcouch.repository.MetaRepoConfig;
import contentcouch.value.BaseRef;

public class OldStyleRdfDirectoryTest extends TestCase
{
	public void setUp() {
		Context.setThreadLocalInstance(MetaRepoConfig.OLD_STYLE_CONFIG);
	}
	
	protected static final String blobEntryString =
		"<DirectoryEntry xmlns=\"http://ns.nuke24.net/ContentCouch/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
		"\t<name>foo</name>\n" +
		"\t<target rdf:resource=\"urn:sha1:3CL2HWDOYOLPUXLWTKY6PAP63D7VOYZO\"/>\n" +
		"\t<targetType>Blob</targetType>\n" +
		"</DirectoryEntry>\n";
	
	public void testBlobEntry() {
		BaseRef target = new BaseRef("urn:sha1:3CL2HWDOYOLPUXLWTKY6PAP63D7VOYZO");
		SimpleDirectory.Entry e = new SimpleDirectory.Entry("foo", target, CCouchNamespace.TT_SHORTHAND_BLOB);
		RdfDirectory.Entry rdfDE = new RdfDirectory.Entry(e, RdfDirectory.DEFAULT_TARGET_RDFIFIER);
		assertEquals( blobEntryString, rdfDE.toXml() );
	}
	
	protected static final String blobWithSizeEntryString =
		"<DirectoryEntry xmlns=\"http://ns.nuke24.net/ContentCouch/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
		"\t<name>foo</name>\n" +
		"\t<size>1234</size>\n" +
		"\t<target rdf:resource=\"urn:sha1:3CL2HWDOYOLPUXLWTKY6PAP63D7VOYZO\"/>\n" +
		"\t<targetType>Blob</targetType>\n" +
		"</DirectoryEntry>\n";
	
	public void testBlobWithSizeEntry() {
		BaseRef target = new BaseRef("urn:sha1:3CL2HWDOYOLPUXLWTKY6PAP63D7VOYZO");
		SimpleDirectory.Entry e = new SimpleDirectory.Entry("foo", target, CCouchNamespace.TT_SHORTHAND_BLOB);
		e.targetSize = 1234;
		RdfDirectory.Entry rdfDE = new RdfDirectory.Entry(e, RdfDirectory.DEFAULT_TARGET_RDFIFIER);
		assertEquals( blobWithSizeEntryString, rdfDE.toXml() );
	}
	
	protected static final String blobWithSizeAndMtimeEntryString =
		"<DirectoryEntry xmlns=\"http://ns.nuke24.net/ContentCouch/\" xmlns:dc=\"http://purl.org/dc/terms/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
		"\t<name>foo</name>\n" +
		"\t<size>1234</size>\n" +
		"\t<target rdf:resource=\"urn:sha1:3CL2HWDOYOLPUXLWTKY6PAP63D7VOYZO\"/>\n" +
		"\t<targetType>Blob</targetType>\n" +
		"\t<dc:modified>2010-01-01 06:00:32 GMT</dc:modified>\n" +
		"</DirectoryEntry>\n";
	
	public void testBlobWithSizeAndMtimeEntry() throws ParseException {
		BaseRef target = new BaseRef("urn:sha1:3CL2HWDOYOLPUXLWTKY6PAP63D7VOYZO");
		SimpleDirectory.Entry e = new SimpleDirectory.Entry("foo", target, CCouchNamespace.TT_SHORTHAND_BLOB);
		e.targetSize = 1234;
		e.lastModified = DateUtil.parseDate("2010-01-01 06:00:32 GMT").getTime();
		RdfDirectory.Entry rdfDE = new RdfDirectory.Entry(e, RdfDirectory.DEFAULT_TARGET_RDFIFIER);
		assertEquals( blobWithSizeAndMtimeEntryString, rdfDE.toXml() );
	}
	
	protected static final String directoryEntryString =
		"<DirectoryEntry xmlns=\"http://ns.nuke24.net/ContentCouch/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
		"\t<name>2010-01</name>\n" +
		"\t<target rdf:resource=\"x-parse-rdf:urn:sha1:AO6BBOUDVIXOV6PEDR7GM536K3WLO6ES\"/>\n" +
		"\t<targetType>Directory</targetType>\n" +
		"</DirectoryEntry>\n";
	
	public void testDirectoryEntry() {
		BaseRef target = new BaseRef("x-parse-rdf:urn:sha1:AO6BBOUDVIXOV6PEDR7GM536K3WLO6ES");
		SimpleDirectory.Entry e = new SimpleDirectory.Entry("2010-01", target, CCouchNamespace.TT_SHORTHAND_DIRECTORY);
		RdfDirectory.Entry rdfDE = new RdfDirectory.Entry(e, RdfDirectory.DEFAULT_TARGET_RDFIFIER);
		assertEquals( directoryEntryString, rdfDE.toXml() );
	}
}
