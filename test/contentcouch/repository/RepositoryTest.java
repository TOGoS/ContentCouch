package contentcouch.repository;

import junit.framework.TestCase;
import togos.rra.BaseRequest;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.blob.BlobUtil;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.SimpleDirectory;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.rdf.RdfDirectory;
import contentcouch.store.TheGetter;
import contentcouch.value.Blob;
import contentcouch.value.Directory;

public class RepositoryTest extends TestCase {
	MetaRepoConfig mrc;
	RepoConfig testRepoConfig = new RepoConfig(RepoConfig.DISPOSITION_LOCAL, "x-memtemp:/test-repo/", "test-repo");
	public void setUp() {
		mrc = new MetaRepoConfig();
		TheGetter.globalInstance = mrc.getRequestKernel();
		mrc.addRepoConfig(testRepoConfig);
	}

	public void testStoreBlob() {
		String testString = "Hello, world!";
		Blob testBlob = BlobUtil.getBlob(testString);
		byte[] hash = testRepoConfig.dataScheme.getHash(testBlob);
		String fn = testRepoConfig.dataScheme.hashToFilename(hash);
		String fp = fn.substring(0,2) + "/" + fn;
		String urn = testRepoConfig.dataScheme.hashToUrn(hash);
		
		TheGetter.put("x-ccouch-repo://test-repo/data", testBlob);
		assertEquals(0, BlobUtil.compareBlobs((Blob)TheGetter.get("x-ccouch-repo://test-repo/data/user/" + fp), testBlob));
		assertEquals(0, BlobUtil.compareBlobs((Blob)TheGetter.get(urn), testBlob));
	}

	/*
	public void testStoreString() {
		String testString = "Hello, world!";
		Blob testBlob = BlobUtil.getBlob(testString);
		byte[] hash = testRepoConfig.dataScheme.getHash(testBlob);
		String fn = testRepoConfig.dataScheme.hashToFilename(hash);
		String fp = fn.substring(0,2) + "/" + fn;
		String urn = testRepoConfig.dataScheme.hashToUrn(hash);
		
		TheGetter.put("x-ccouch-repo://test-repo/data", testString);
		assertEquals(0, BlobUtil.compareBlobs((Blob)TheGetter.get("x-ccouch-repo://test-repo/data/user/" + fp), testBlob));
		assertEquals(0, BlobUtil.compareBlobs((Blob)TheGetter.get(urn), testBlob));
	}
	*/
	
	protected SimpleDirectory.Entry createBlobDirectoryEntry(String name, String content) {
		SimpleDirectory.Entry e = new SimpleDirectory.Entry();
		e.name = name;
		e.target = BlobUtil.getBlob(content);
		e.targetType = CcouchNamespace.OBJECT_TYPE_BLOB;
		e.targetLastModified = 1000l;
		e.targetSize = ((Blob)e.target).getLength();
		return e;
	}
	
	protected SimpleDirectory createTestSimpleDirectory() {
		SimpleDirectory simpleDir = new SimpleDirectory();
		simpleDir.addDirectoryEntry(createBlobDirectoryEntry("hello1", "Hello, world!"));
		simpleDir.addDirectoryEntry(createBlobDirectoryEntry("hello2", "Hello again, world!"));
		return simpleDir;
	}
	
	protected String storeDirectory(Directory dir) {
		BaseRequest putReq = new BaseRequest(Request.VERB_PUT, "x-ccouch-repo://test-repo/data");
		putReq.content = dir;
		
		Response putRes = TheGetter.handleRequest(putReq);
		TheGetter.getResponseValue(putRes, putReq);
		return MetadataUtil.getStoredIdentifier(putRes);
	}
	
	public void testStoreSimpleDirectory() {
		String storedUri = storeDirectory(createTestSimpleDirectory());
		
		assertNotNull( storedUri );
		assertTrue( storedUri.startsWith("x-parse-rdf:urn:sha1:") );

		System.err.println(storedUri);
		System.err.println(BlobUtil.getString((Blob)TheGetter.get(storedUri.substring("x-parse-rdf:".length()))));
		
		Directory storedDir = (Directory)TheGetter.get(storedUri);
		assertTrue( storedDir instanceof RdfDirectory );
		assertEquals( 2, storedDir.getDirectoryEntrySet().size() );
	}

	public void testStoreRdfDirectory() {
		String originalStoredUri = storeDirectory(createTestSimpleDirectory());
		RdfDirectory rdfDir = (RdfDirectory)TheGetter.get(originalStoredUri);
		assertTrue( rdfDir instanceof RdfDirectory );
		
		BaseRequest putReq = new BaseRequest(Request.VERB_PUT, "x-ccouch-repo://test-repo/data");
		putReq.content = rdfDir;
		putReq.putMetadata(CcouchNamespace.REQ_STORE_SECTOR, "bilge");
		Response putRes = TheGetter.handleRequest(putReq);
		
		TheGetter.getResponseValue(putRes, putReq);
		String rdfStoredUri = MetadataUtil.getStoredIdentifier(putRes);
		// Ensure that the URN of the re-stored RDF is the same as the original
		assertEquals( originalStoredUri, rdfStoredUri );
		
		// Ensure that hello1 blob was actually stored in the 'bilge' sector
		Blob bilgeBlob = (Blob)TheGetter.get("x-ccouch-repo://test-repo/data/bilge/SQ/SQ5HALIG6NCZTLXB7DNI56PXFFQDDVUZ");
		assertNotNull( bilgeBlob );
		assertEquals("Hello, world!", BlobUtil.getString(bilgeBlob));
	}
	
	public void testUriDotFilesStored() {
		SimpleDirectory simpleDir = createTestSimpleDirectory();
		
		BaseRequest storeReq = new BaseRequest(Request.VERB_PUT, "x-ccouch-repo://test-repo/data");
		storeReq.content = simpleDir;
		storeReq.putMetadata(CcouchNamespace.REQ_CREATE_URI_DOT_FILES, Boolean.TRUE);
		Response storeRes = TheGetter.handleRequest(storeReq);
		String originalStoredUri = MetadataUtil.getStoredIdentifier(storeRes);
		
		assertNotNull(originalStoredUri);
		assertNotNull(simpleDir.getDirectoryEntry(".ccouch-uri"));
		
		// Add a new entry to make the previous URI no longer apply
		simpleDir.addDirectoryEntry(createBlobDirectoryEntry("hello3", "A third hello!"));
		
		// Using uri dot files, we should still get the old URI
		storeReq = new BaseRequest(Request.VERB_PUT, "x-ccouch-repo://test-repo/data");
		storeReq.content = simpleDir;
		storeReq.putMetadata(CcouchNamespace.REQ_CREATE_URI_DOT_FILES, Boolean.TRUE);
		storeReq.putMetadata(CcouchNamespace.REQ_USE_URI_DOT_FILES, Boolean.TRUE);
		storeRes = TheGetter.handleRequest(storeReq);
		String incorrectStoredUri = MetadataUtil.getStoredIdentifier(storeRes);
		
		assertEquals( originalStoredUri, incorrectStoredUri );

		// Without using uri dot files, we should get a new URI
		simpleDir.addDirectoryEntry(createBlobDirectoryEntry("hello3", "A third hello!"));
		storeReq = new BaseRequest(Request.VERB_PUT, "x-ccouch-repo://test-repo/data");
		storeReq.content = simpleDir;
		storeReq.putMetadata(CcouchNamespace.REQ_CREATE_URI_DOT_FILES, Boolean.TRUE);
		storeReq.putMetadata(CcouchNamespace.REQ_USE_URI_DOT_FILES, Boolean.FALSE);
		storeRes = TheGetter.handleRequest(storeReq);
		String newStoredUri = MetadataUtil.getStoredIdentifier(storeRes);
		
		assertFalse( newStoredUri.equals(originalStoredUri) );
	}
}
