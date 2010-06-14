package contentcouch.merge;

import junit.framework.TestCase;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.base.BaseRequest;
import contentcouch.blob.BlobUtil;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.SimpleCommit;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.rdf.RdfCommit;
import contentcouch.rdf.RdfDirectory;
import contentcouch.repository.MetaRepoConfig;
import contentcouch.repository.RepoConfig;
import contentcouch.store.TheGetter;
import contentcouch.value.BaseRef;
import contentcouch.value.Commit;
import contentcouch.value.Ref;

public class MergeUtilTest extends TestCase {
	MetaRepoConfig mrc;
	RepoConfig testRepoConfig = new RepoConfig(RepoConfig.DISPOSITION_LOCAL, "x-memtemp:/test-repo/", "test-repo");
	public void setUp() {
		mrc = new MetaRepoConfig();
		TheGetter.globalInstance = mrc.getRequestKernel();
		mrc.addRepoConfig(testRepoConfig);
	}
	
	protected Ref storeCommit(Commit c) {
		RdfCommit rdfCommit = new RdfCommit(c, RdfDirectory.DEFAULT_TARGET_RDFIFIER);
		BaseRequest storeCommitReq = TheGetter.createRequest(RequestVerbs.VERB_PUT, "x-ccouch-repo://test-repo/data");
		storeCommitReq.content = BlobUtil.getBlob(rdfCommit.toString());
		storeCommitReq.putMetadata(CcouchNamespace.REQ_FILEMERGE_METHOD, CcouchNamespace.REQ_FILEMERGE_STRICTIG);
		Response storeCommitRes = TheGetter.callAndThrowIfNonNormalResponse(storeCommitReq);
		String commitBlobUrn = MetadataUtil.getStoredIdentifier(storeCommitRes);
		return new BaseRef( CcouchNamespace.RDF_SUBJECT_URI_PREFIX + commitBlobUrn );
	}
	
	protected Ref storeCommit( Object target, Object[] parents ) {
		SimpleCommit sc = new SimpleCommit();
		sc.target = target;
		sc.parents = parents;
		return storeCommit(sc);
	}
	
	public void testStoreCommit() {
		SimpleCommit sc = new SimpleCommit();
		assertTrue( storeCommit(sc).getTargetUri().startsWith(CcouchNamespace.RDF_SUBJECT_URI_PREFIX+"urn:") );
	}
	
	protected void assertTrue( Boolean value ) {
		assertNotNull(value);
		assertTrue(value.booleanValue());
	}

	protected void assertFalse( Boolean value ) {
		assertNotNull(value);
		assertFalse(value.booleanValue());
	}

	protected void assertNotNecessarilyTrue( Boolean value ) {
		if( value != null ) {
			assertFalse(value.booleanValue());
		}
	}

	public void testUriEquivalence() {
		assertTrue( MergeUtil.areUrisEquivalent( "urn:sha1:RSCTFCQ6OUTZ2ETQPX2ZP3542WG4DY7V", "urn:sha1:RSCTFCQ6OUTZ2ETQPX2ZP3542WG4DY7V" ) );
		assertFalse( MergeUtil.areUrisEquivalent( "urn:sha1:RSCTFCQ6OUTZ2ETQPX2ZP3542WG4DY7V", "urn:sha1:XXXXFCQ6OUTZ2ETQPX2ZP3542WG4DY7V" ) );
		assertTrue( MergeUtil.areUrisEquivalent( "x-parse-rdf:urn:sha1:RSCTFCQ6OUTZ2ETQPX2ZP3542WG4DY7V", "x-rdf-subject:urn:sha1:RSCTFCQ6OUTZ2ETQPX2ZP3542WG4DY7V" ) );
		assertNotNecessarilyTrue( MergeUtil.areUrisEquivalent( "x-rdf-subject:urn:sha1:RSCTFCQ6OUTZ2ETQPX2ZP3542WG4DY7V", "urn:sha1:RSCTFCQ6OUTZ2ETQPX2ZP3542WG4DY7V" ) );
	}
	
	public void testUrnEquivalence() {
		assertTrue( MergeUtil.areUrisEquivalent(
			"urn:sha1:RSCTFCQ6OUTZ2ETQPX2ZP3542WG4DY7V",
			"urn:bitprint:RSCTFCQ6OUTZ2ETQPX2ZP3542WG4DY7V.TFUAXNIEPBE53C4KFYZVZNK3OU3E5MAVHSQPAEI" ) );
		assertFalse( MergeUtil.areUrisEquivalent(
			"urn:sha1:RSCTFCQ6OUTZ2ETQPX2ZP3542WG4DY7V",
			"urn:bitprint:XXXXFCQ6OUTZ2ETQPX2ZP3542WG4DY7V.TFUAXNIEPBE53C4KFYZVZNK3OU3E5MAVHSQPAEI" ) );
		assertTrue( MergeUtil.areUrisEquivalent(
			"urn:tree:tiger:TFUAXNIEPBE53C4KFYZVZNK3OU3E5MAVHSQPAEI",
			"urn:bitprint:RSCTFCQ6OUTZ2ETQPX2ZP3542WG4DY7V.TFUAXNIEPBE53C4KFYZVZNK3OU3E5MAVHSQPAEI" ) );
		assertFalse( MergeUtil.areUrisEquivalent(
			"urn:tree:tiger:TFUAXNIEPBE53C4KFYZVZNK3OU3E5MAVHSQPAEI",
			"urn:bitprint:RSCTFCQ6OUTZ2ETQPX2ZP3542WG4DY7V.XXXXXNIEPBE53C4KFYZVZNK3OU3E5MAVHSQPAEI" ) );
		assertNull( MergeUtil.areUrisEquivalent(
			"urn:sha1:RSCTFCQ6OUTZ2ETQPX2ZP3542WG4DY7V",
			"urn:tree:tiger:TFUAXNIEPBE53C4KFYZVZNK3OU3E5MAVHSQPAEI" ) );
	}
	
	public void xxx_testFindCommonParent() {
		Ref parentRef = storeCommit(new BaseRef("urn:sha1:FOO"),null);
		Ref c1Ref = storeCommit(new BaseRef("urn:sha1:BAR"),new Object[]{parentRef});
		Ref c2Ref = storeCommit(new BaseRef("urn:sha1:BAZ"),new Object[]{parentRef});
		
		assertFalse( parentRef.getTargetUri().equals(c1Ref.getTargetUri()));
		assertFalse( parentRef.getTargetUri().equals(c2Ref.getTargetUri()));
		assertFalse( c1Ref.getTargetUri().equals(c2Ref.getTargetUri()));
		
		assertEquals( parentRef.getTargetUri(), MergeUtil.findCommonAncestor(c1Ref.getTargetUri(), c2Ref.getTargetUri()) );
	}

}
