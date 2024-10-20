package contentcouch.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import contentcouch.blob.Blob;
import contentcouch.blob.BlobUtil;
import contentcouch.commit.SimpleCommit;
import contentcouch.contentaddressing.ContentAddressingScheme;
import contentcouch.contentaddressing.TigerTreeScheme;
import contentcouch.context.Context;
import contentcouch.directory.SimpleDirectory;
import contentcouch.file.FileDirectory;
import contentcouch.framework.TheGetter;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.UriUtil;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.rdf.RdfCommit;
import contentcouch.rdf.RdfDirectory;
import contentcouch.value.BaseRef;
import contentcouch.value.Commit;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class RepositoryTest extends TestCase {
	MetaRepoConfig mrc;
	RepoConfig testRepoConfig = new RepoConfig(RepoConfig.DISPOSITION_DEFAULT, "test-repo", "x-memtemp:/test-repo/");
	RepoConfig testRemoteRepoConfig = new RepoConfig(RepoConfig.DISPOSITION_REMOTE, "test-remote-repo", "x-memtemp:/test-remote-repo/");
	public void setUp() {
		Context.setThreadLocalInstance(new HashMap());
		mrc = new MetaRepoConfig();
		// Since there's no actual directory, SLF files don't work,
		// and we need to override the cache thingy in order for tests
		// that rely on caching working to pass:
		mrc.stringCacheOverride = new HashMap();
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

		getReq = new BaseRequest(RequestVerbs.GET, storageUrn );
		Response res = TheGetter.call( getReq );
		assertNotNull( TheGetter.getResponseValue(res,getReq) );
		
		assertNull( TheGetter.get("x-memtemp:/test-repo/data/user/"+storageFilePostfix) );
		assertNull( TheGetter.get("x-memtemp:/test-repo/data/remote/"+storageFilePostfix) );
		assertNull( TheGetter.get("x-memtemp:/test-repo/data/kash/"+storageFilePostfix) );

		getReq = new BaseRequest(RequestVerbs.GET, storageUrn );
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
		BaseRequest putReq = new BaseRequest(RequestVerbs.PUT, "x-ccouch-repo://test-repo/data");
		putReq.content = dir;
		
		Response putRes = TheGetter.call(putReq);
		TheGetter.getResponseValue(putRes, putReq);
		return MetadataUtil.getStoredIdentifier(putRes);
	}
	
	public void testStoreSimpleDirectory() {
		String storedUri = storeDirectory(createTestSimpleDirectory());
		
		assertNotNull( storedUri );
		assertTrue( "'"+storedUri+"' does not start with '"+CCouchNamespace.RDF_SUBJECT_URI_PREFIX+"'", storedUri.startsWith(CCouchNamespace.RDF_SUBJECT_URI_PREFIX));

		Directory storedDir = (Directory)TheGetter.get(storedUri);
		assertTrue( storedDir instanceof RdfDirectory );
		assertEquals( 2, storedDir.getDirectoryEntrySet().size() );
	}

	public void testStoreRdfDirectory() {
		String originalStoredUri = storeDirectory(createTestSimpleDirectory());
		RdfDirectory rdfDir = (RdfDirectory)TheGetter.get(originalStoredUri);
		assertTrue( rdfDir instanceof RdfDirectory );
		
		BaseRequest putReq = new BaseRequest(RequestVerbs.PUT, "x-ccouch-repo://test-repo/data");
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
		
		BaseRequest storeReq = new BaseRequest(RequestVerbs.PUT, "x-ccouch-repo://test-repo/data");
		storeReq.content = simpleDir;
		storeReq.putMetadata(CCouchNamespace.REQ_CREATE_URI_DOT_FILES, Boolean.TRUE);
		Response storeRes = TheGetter.call(storeReq);
		String originalStoredUri = MetadataUtil.getStoredIdentifier(storeRes);
		
		assertNotNull(originalStoredUri);
		assertNotNull(simpleDir.getDirectoryEntry(".ccouch-uri"));
		
		// Add a new entry to make the previous URI no longer apply
		simpleDir.addDirectoryEntry(createBlobDirectoryEntry("hello3", "A third hello!"));
		
		// Using uri dot files, we should still get the old URI
		storeReq = new BaseRequest(RequestVerbs.PUT, "x-ccouch-repo://test-repo/data");
		storeReq.content = simpleDir;
		storeReq.putMetadata(CCouchNamespace.REQ_CREATE_URI_DOT_FILES, Boolean.TRUE);
		storeReq.putMetadata(CCouchNamespace.REQ_USE_URI_DOT_FILES, Boolean.TRUE);
		storeRes = TheGetter.call(storeReq);
		String incorrectStoredUri = MetadataUtil.getStoredIdentifier(storeRes);
		
		assertEquals( originalStoredUri, incorrectStoredUri );

		// Without using uri dot files, we should get a new URI
		simpleDir.addDirectoryEntry(createBlobDirectoryEntry("hello3", "A third hello!"));
		storeReq = new BaseRequest(RequestVerbs.PUT, "x-ccouch-repo://test-repo/data");
		storeReq.content = simpleDir;
		storeReq.putMetadata(CCouchNamespace.REQ_CREATE_URI_DOT_FILES, Boolean.TRUE);
		storeReq.putMetadata(CCouchNamespace.REQ_USE_URI_DOT_FILES, Boolean.FALSE);
		storeRes = TheGetter.call(storeReq);
		String newStoredUri = MetadataUtil.getStoredIdentifier(storeRes);
		
		assertFalse( newStoredUri.equals(originalStoredUri) );
	}
	
	protected Ref storeCommit(Commit c) {
		RdfCommit rdfCommit = new RdfCommit(c, RdfDirectory.DEFAULT_TARGET_RDFIFIER);
		BaseRequest storeCommitReq = TheGetter.createRequest(RequestVerbs.PUT, "x-ccouch-repo://test-repo/data");
		storeCommitReq.content = BlobUtil.getBlob(rdfCommit.toXml());
		storeCommitReq.putMetadata(CCouchNamespace.REQ_FILEMERGE_METHOD, CCouchNamespace.REQ_FILEMERGE_STRICTIG);
		Response storeCommitRes = TheGetter.callAndThrowIfNonNormalResponse(storeCommitReq);
		String commitBlobUrn = MetadataUtil.getStoredIdentifier(storeCommitRes);
		return new BaseRef( CCouchNamespace.RDF_SUBJECT_URI_PREFIX + commitBlobUrn );
	}
	
	protected boolean exists( String uri ) {
		Response res = TheGetter.call( new BaseRequest(RequestVerbs.GET,uri) );
		switch( res.getStatus() ) { 
		case( ResponseCodes.NORMAL ): return true;
		case( ResponseCodes.DOES_NOT_EXIST ): return false;
		default: throw new RuntimeException("Expected 200 or 400, got "+res.getStatus() );
		}
	}
	
	protected void assertExists( String uri ) {
		assertTrue( exists(uri) );
	}
	
	protected void assertNotExists( String uri ) {
		assertFalse( exists(uri) );
	}
	
	static Pattern GHA = Pattern.compile("^x-rdf-subject:urn:sha1:([A-Z0-9]+)$");
	
	protected String extractDataFilePath( String uri ) {
		Matcher m = GHA.matcher(uri);
		if( m.matches() ) {
			String b32 = m.group(1);
			if( b32 == null ) return null;
			return b32.substring(0,2)+"/"+b32;
		} else{
			return null;
		}
	}
	
	protected String getStoreUri( String sector, String urn ) {
		return "x-ccouch-repo://test-repo/files/data/" +
			sector + "/" + extractDataFilePath(urn);
	}
	
	public void testAncestorCommitsStored() {
		Object[] parents = new Object[0];
		Ref commitRef = null;
		ArrayList commitRefs = new ArrayList();
		
		for( int i=0; i<100; ++i ) {
			SimpleCommit c = new SimpleCommit();
			c.target = BlobUtil.getBlob("Hwllo "+i+"!");
			TheGetter.put("x-ccouch-repo://test-repo/data", c.target);
			c.parents = parents;
			commitRef = storeCommit(c);
			commitRefs.add(0, commitRef);
			parents = new Object[]{commitRef};
		}
		
		int[] followCounts = new int[]{1,40,80,99};
		
		for( int i=0; i<followCounts.length; ++i ) {
			int followCount = followCounts[i];
			
			BaseRequest getReq = TheGetter.createRequest( RequestVerbs.GET, commitRef.getTargetUri() );
			Response getRes = TheGetter.call(getReq);
			Commit mostRecentCommit = (Commit)TheGetter.getResponseValue(getRes, getReq);
			
			String sector = "last"+followCount; 
			
			BaseRequest putReq = TheGetter.createRequest( RequestVerbs.PUT, "x-ccouch-repo://test-repo/data/"+sector );
			putReq.content = mostRecentCommit;
			putReq.contentMetadata = getRes.getContentMetadata();
			putReq.putMetadata(CCouchNamespace.REQ_CACHE_COMMIT_ANCESTORS, new Integer(followCount));
			TheGetter.callAndThrowIfNonNormalResponse(putReq);
			
			// #0 should always be in there:
			assertExists( getStoreUri(sector, ((Ref)commitRefs.get(0)).getTargetUri()) );
			for( int j=1; j<=followCount; ++j ) {
				String storeUri = getStoreUri(sector, ((Ref)commitRefs.get(j)).getTargetUri());
				assertExists( storeUri );
			}
			for( int j=followCount+1; j<commitRefs.size(); ++j ) {
				String storeUri = getStoreUri(sector, ((Ref)commitRefs.get(j)).getTargetUri());
				assertNotExists( storeUri );
			}
		}
	}
	
	public void testDirectoryHashCaching() {
		ContentAddressingScheme scheme = TigerTreeScheme.getInstance();
		
		File testRepoDir = new File("junk/test-repo");
		File configFile = new File("junk/test-repo/ccouch-config");
		File hashCacheFile = new File("junk/test-repo/cache/file-"+scheme.getSchemeShortName()+".slf");
		File dirToIdentify = new File("junk/more-junk" );
		if( configFile.exists() ) configFile.delete();
		if( hashCacheFile.exists() ) hashCacheFile.delete();
		if( !testRepoDir.exists() ) testRepoDir.mkdirs();
		if( !dirToIdentify.exists() ) dirToIdentify.mkdirs();
		
		MetaRepoConfig conf = new MetaRepoConfig();
		conf.defaultRepoConfig = new RepoConfig(RepoConfig.DISPOSITION_DEFAULT, "test", "file:junk/test-repo/");
		MetaRepository mr = new MetaRepository(conf);
		
		Map options = new HashMap();
		options.put( CCouchNamespace.CFG_ID_SCHEME, scheme.getSchemeShortName() );
		options.put( CCouchNamespace.REQ_CACHE_DIRECTORY_HASHES, Boolean.FALSE );
		Context.setThreadLocalInstance( options );
		
		assertNull( mr.getFileHashCache(scheme).getCachedUrn(dirToIdentify) );
		
		String dirId = mr.identifyDirectory(new FileDirectory(dirToIdentify), options);
		String dirDataId = UriUtil.stripRdfSubjectPrefix(dirId);
		
		assertNull( mr.getFileHashCache(scheme).getCachedUrn(dirToIdentify) );
		
		options.put( CCouchNamespace.REQ_CACHE_DIRECTORY_HASHES, Boolean.TRUE );
		
		mr.identifyDirectory(new FileDirectory(dirToIdentify), options);
		
		assertNotNull( mr.getFileHashCache(scheme).getCachedUrn(dirToIdentify) );
		assertEquals( dirDataId, mr.getFileHashCache(scheme).getCachedUrn(dirToIdentify) );
		
		// This doesn't test that the cached value was actually used...
	}
}
