package contentcouch.repository;

import contentcouch.blob.BlobUtil;
import contentcouch.store.TheGetter;
import contentcouch.value.Blob;
import junit.framework.TestCase;

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
}
