package contentcouch.repository;

import java.util.Collections;

import junit.framework.TestCase;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.base.BaseRequest;
import togos.mf.value.Blob;
import contentcouch.blob.BlobUtil;
import contentcouch.directory.SimpleDirectory;
import contentcouch.misc.MemTempRequestHandler;
import contentcouch.misc.MetadataUtil;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.rdf.RdfDirectory;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;

public class RepositoryTest extends TestCase {
	MetaRepoConfig mrc;
	RepoConfig testRepoConfig = new RepoConfig(RepoConfig.DISPOSITION_DEFAULT, "x-memtemp:/test-repo/", "test-repo");
	RepoConfig testRemoteRepoConfig = new RepoConfig(RepoConfig.DISPOSITION_REMOTE, "x-memtemp:/test-remote-repo/", "test-remote-repo");
	public void setUp() {
		mrc = new MetaRepoConfig();
		MemTempRequestHandler.clear();
		TheGetter.globalInstance = mrc.getRequestKernel();
		mrc.addRepoConfig( testRepoConfig );
		mrc.addRepoConfig( testRemoteRepoConfig );
	}

	public void testStoreBlob() {
		String testString = "Hello, world!";
		Blob testBlob = BlobUtil.getBlob(testString);
		byte[] storageHash = testRepoConfig.storageScheme.getHash(testBlob);
		String storageFilename = testRepoConfig.storageScheme.hashToFilename(storageHash);
		String storageFilePostfix = storageFilename.substring(0,2) + "/" + storageFilename;
		String storageUrn = testRepoConfig.storageScheme.hashToUrn(storageHash);
		
		BaseRequest putReq = new BaseRequest("PUT", "x-ccouch-repo://test-repo/data", testBlob, Collections.EMPTY_MAP);
		Response putRes = TheGetter.call( putReq );
		String returnedUrn = (String)putRes.getMetadata().get(CCouchNamespace.RES_STORED_IDENTIFIER);
		//System.err.println("Stored as "+storedUrn+", storage scheme called it "+urn);
		assertEquals(0, BlobUtil.compareBlobs((Blob)TheGetter.get("x-ccouch-repo://test-repo/files/data/user/" + storageFilePostfix), testBlob));
		assertEquals(0, BlobUtil.compareBlobs((Blob)TheGetter.get(storageUrn), testBlob));
		assertEquals(0, BlobUtil.compareBlobs((Blob)TheGetter.get(returnedUrn), testBlob));
	}
	
	public void testCacheNativeBlob() {
		String testString = "Hello, world!";
		Blob testBlob = BlobUtil.getBlob(testString);
		byte[] storageHash = testRepoConfig.storageScheme.getHash(testBlob);
		String storageFilename = testRepoConfig.storageScheme.hashToFilename(storageHash);
		String storageFilePostfix = storageFilename.substring(0,2) + "/" + storageFilename;
		String storageUrn = testRepoConfig.storageScheme.hashToUrn(storageHash);
		
		TheGetter.put( "x-memtemp:/test-remote-repo/data/user/"+storageFilePostfix, testBlob);
		
		BaseRequest getReq;

		getReq = new BaseRequest(RequestVerbs.VERB_GET, storageUrn );
		Response res = TheGetter.call( getReq );
		assertNotNull( TheGetter.getResponseValue(res,getReq) );
		
		assertNull( TheGetter.get("x-memtemp:/test-repo/data/user/"+storageFilePostfix) );
		assertNull( TheGetter.get("x-memtemp:/test-repo/data/remote/"+storageFilePostfix) );
		assertNull( TheGetter.get("x-memtemp:/test-repo/data/kash/"+storageFilePostfix) );

		getReq = new BaseRequest(RequestVerbs.VERB_GET, storageUrn );
		getReq.putMetadata( CCouchNamespace.REQ_CACHE_SECTOR, "kash" );
		res = TheGetter.call( getReq );
		assertNotNull( TheGetter.getResponseValue(res,getReq) );

		assertNull( TheGetter.get("x-memtemp:/test-repo/data/user/"+storageFilePostfix) );
		assertNull( TheGetter.get("x-memtemp:/test-repo/data/remote/"+storageFilePostfix) );
		assertNotNull( TheGetter.get("x-memtemp:/test-repo/data/kash/"+storageFilePostfix) );
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
		e.targetType = CCouchNamespace.TT_SHORTHAND_BLOB;
		e.lastModified = 1000l;
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
		BaseRequest putReq = new BaseRequest(RequestVerbs.VERB_PUT, "x-ccouch-repo://test-repo/data");
		putReq.content = dir;
		
		Response putRes = TheGetter.call(putReq);
		TheGetter.getResponseValue(putRes, putReq);
		return MetadataUtil.getStoredIdentifier(putRes);
	}
	
	public void testStoreSimpleDirectory() {
		String storedUri = storeDirectory(createTestSimpleDirectory());
		
		assertNotNull( storedUri );
		assertTrue( "'"+storedUri+"' does not start with 'x-parse-rdf:'", storedUri.startsWith("x-parse-rdf:"));

		Directory storedDir = (Directory)TheGetter.get(storedUri);
		assertTrue( storedDir instanceof RdfDirectory );
		assertEquals( 2, storedDir.getDirectoryEntrySet().size() );
	}

	public void testStoreRdfDirectory() {
		String originalStoredUri = storeDirectory(createTestSimpleDirectory());
		RdfDirectory rdfDir = (RdfDirectory)TheGetter.get(originalStoredUri);
		assertTrue( rdfDir instanceof RdfDirectory );
		
		BaseRequest putReq = new BaseRequest(RequestVerbs.VERB_PUT, "x-ccouch-repo://test-repo/data");
		putReq.content = rdfDir;
		putReq.putMetadata(CCouchNamespace.REQ_STORE_SECTOR, "bilge");
		Response putRes = TheGetter.call(putReq);
		
		TheGetter.getResponseValue(putRes, putReq);
		String rdfStoredUri = MetadataUtil.getStoredIdentifier(putRes);
		// Ensure that the URN of the re-stored RDF is the same as the original
		assertEquals( originalStoredUri, rdfStoredUri );
		
		// Ensure that hello1 blob was actually stored in the 'bilge' sector
		Blob bilgeBlob = (Blob)TheGetter.get("x-ccouch-repo://test-repo/files/data/bilge/SQ/SQ5HALIG6NCZTLXB7DNI56PXFFQDDVUZ");
		assertNotNull( bilgeBlob );
		assertEquals("Hello, world!", BlobUtil.getString(bilgeBlob));
	}
	
	public void testUriDotFilesStored() {
		SimpleDirectory simpleDir = createTestSimpleDirectory();
		
		BaseRequest storeReq = new BaseRequest(RequestVerbs.VERB_PUT, "x-ccouch-repo://test-repo/data");
		storeReq.content = simpleDir;
		storeReq.putMetadata(CCouchNamespace.REQ_CREATE_URI_DOT_FILES, Boolean.TRUE);
		Response storeRes = TheGetter.call(storeReq);
		String originalStoredUri = MetadataUtil.getStoredIdentifier(storeRes);
		
		assertNotNull(originalStoredUri);
		assertNotNull(simpleDir.getDirectoryEntry(".ccouch-uri"));
		
		// Add a new entry to make the previous URI no longer apply
		simpleDir.addDirectoryEntry(createBlobDirectoryEntry("hello3", "A third hello!"));
		
		// Using uri dot files, we should still get the old URI
		storeReq = new BaseRequest(RequestVerbs.VERB_PUT, "x-ccouch-repo://test-repo/data");
		storeReq.content = simpleDir;
		storeReq.putMetadata(CCouchNamespace.REQ_CREATE_URI_DOT_FILES, Boolean.TRUE);
		storeReq.putMetadata(CCouchNamespace.REQ_USE_URI_DOT_FILES, Boolean.TRUE);
		storeRes = TheGetter.call(storeReq);
		String incorrectStoredUri = MetadataUtil.getStoredIdentifier(storeRes);
		
		assertEquals( originalStoredUri, incorrectStoredUri );

		// Without using uri dot files, we should get a new URI
		simpleDir.addDirectoryEntry(createBlobDirectoryEntry("hello3", "A third hello!"));
		storeReq = new BaseRequest(RequestVerbs.VERB_PUT, "x-ccouch-repo://test-repo/data");
		storeReq.content = simpleDir;
		storeReq.putMetadata(CCouchNamespace.REQ_CREATE_URI_DOT_FILES, Boolean.TRUE);
		storeReq.putMetadata(CCouchNamespace.REQ_USE_URI_DOT_FILES, Boolean.FALSE);
		storeRes = TheGetter.call(storeReq);
		String newStoredUri = MetadataUtil.getStoredIdentifier(storeRes);
		
		assertFalse( newStoredUri.equals(originalStoredUri) );
	}
}
