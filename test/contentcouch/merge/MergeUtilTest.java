package contentcouch.merge;

import junit.framework.TestCase;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.base.BaseRequest;
import contentcouch.blob.BlobUtil;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.SimpleCommit;
import contentcouch.misc.SimpleDirectory;
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
	protected static String SHA1A = "urn:sha1:AAAAFCQ6OUTZ2ETQPX2ZP3542WG4DY7V";
	protected static String SHA1B = "urn:sha1:BBBBFCQ6OUTZ2ETQPX2ZP3542WG4DY7V";
	protected static String SHA1C = "urn:sha1:CCCCFCQ6OUTZ2ETQPX2ZP3542WG4DY7V";
	protected static String SHA1D = "urn:sha1:DDDDFCQ6OUTZ2ETQPX2ZP3542WG4DY7V";
	protected static String SHA1E = "urn:sha1:EEEEFCQ6OUTZ2ETQPX2ZP3542WG4DY7V";
	protected static String SHA1F = "urn:sha1:FFFFFCQ6OUTZ2ETQPX2ZP3542WG4DY7V";
	protected static String SHA1G = "urn:sha1:GGGGFCQ6OUTZ2ETQPX2ZP3542WG4DY7V";
	protected static String SHA1H = "urn:sha1:HHHHFCQ6OUTZ2ETQPX2ZP3542WG4DY7V";
	protected static String SHA1I = "urn:sha1:IIIIFCQ6OUTZ2ETQPX2ZP3542WG4DY7V";
	protected static String SHA1J = "urn:sha1:JJJJFCQ6OUTZ2ETQPX2ZP3542WG4DY7V";
	
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
		assertTrue( MergeUtil.areUrisEquivalent( SHA1A, SHA1A ) );
		assertFalse( MergeUtil.areUrisEquivalent( SHA1A, SHA1B ) );
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
	
	public void testFindCommonParent() {
		Ref parentRef = storeCommit(new BaseRef(SHA1A),null);
		Ref c1Ref = storeCommit(new BaseRef(SHA1B),new Object[]{parentRef});
		Ref c2Ref = storeCommit(new BaseRef(SHA1C),new Object[]{parentRef});
		
		assertFalse( parentRef.getTargetUri().equals(c1Ref.getTargetUri()));
		assertFalse( parentRef.getTargetUri().equals(c2Ref.getTargetUri()));
		assertFalse( c1Ref.getTargetUri().equals(c2Ref.getTargetUri()));
		
		assertEquals( parentRef.getTargetUri(), MergeUtil.findCommonAncestor(c1Ref.getTargetUri(), c2Ref.getTargetUri()) );
	}

	public void testFindCommonParentOfSameCommit() {
		Ref parentRef = storeCommit(new BaseRef(SHA1A),null);
		
		assertEquals( parentRef.getTargetUri(), MergeUtil.findCommonAncestor(parentRef.getTargetUri(), parentRef.getTargetUri()) );
	}

	public void testFindCommonParentInLinearChain() {
		Ref   rootRef = storeCommit(new BaseRef(SHA1D),new Object[]{});
		Ref childARef = storeCommit(new BaseRef(SHA1A),new Object[]{rootRef});
		Ref childBRef = storeCommit(new BaseRef(SHA1B),new Object[]{childARef});
		Ref childCRef = storeCommit(new BaseRef(SHA1C),new Object[]{childBRef});
		
		assertEquals( rootRef.getTargetUri(), MergeUtil.findCommonAncestor(rootRef.getTargetUri(), childARef.getTargetUri()) );
		assertEquals( rootRef.getTargetUri(), MergeUtil.findCommonAncestor(rootRef.getTargetUri(), childBRef.getTargetUri()) );
		assertEquals( rootRef.getTargetUri(), MergeUtil.findCommonAncestor(rootRef.getTargetUri(), childCRef.getTargetUri()) );
		
		assertEquals( childBRef.getTargetUri(), MergeUtil.findCommonAncestor(childBRef.getTargetUri(), childCRef.getTargetUri()) );
	}

	public void testFindCommonParentInComplex() {
		Ref aRef = storeCommit(new BaseRef(SHA1A),new Object[]{});
		Ref bRef = storeCommit(new BaseRef(SHA1B),new Object[]{aRef});
		Ref cRef = storeCommit(new BaseRef(SHA1C),new Object[]{aRef});
		Ref dRef = storeCommit(new BaseRef(SHA1D),new Object[]{bRef});
		Ref eRef = storeCommit(new BaseRef(SHA1E),new Object[]{bRef});
		Ref fRef = storeCommit(new BaseRef(SHA1F),new Object[]{dRef,eRef});
		
		assertEquals( aRef.getTargetUri(), MergeUtil.findCommonAncestor(cRef.getTargetUri(), fRef.getTargetUri()) );
		assertEquals( bRef.getTargetUri(), MergeUtil.findCommonAncestor(dRef.getTargetUri(), eRef.getTargetUri()) );
		assertEquals( eRef.getTargetUri(), MergeUtil.findCommonAncestor(eRef.getTargetUri(), fRef.getTargetUri()) );
	}
	
	////
	
	public void testChangesetDump() {
		Changeset cs = new Changeset();
		cs.addChange(new FileAdd("foo",new BaseRef(SHA1A),null));
		cs.addChange(new FileAdd("bar",new BaseRef(SHA1B),new FileDelete("bar",null)));
		assertEquals(
			"D  bar\n"+
			"A  bar\n"+
			"A  foo\n",
			cs.dump()
		);
	}
	
	static SimpleDirectory oldDir;
	static SimpleDirectory newDir;
	static {
		SimpleDirectory oldSubDir1 = new SimpleDirectory();
		oldSubDir1.addDirectoryEntry(new SimpleDirectory.Entry("blobc", new BaseRef(SHA1C), CcouchNamespace.TT_SHORTHAND_BLOB));
		oldSubDir1.addDirectoryEntry(new SimpleDirectory.Entry("blobd", new BaseRef(SHA1D), CcouchNamespace.TT_SHORTHAND_BLOB));
		SimpleDirectory oldSubDir2 = new SimpleDirectory();
		oldSubDir2.addDirectoryEntry(new SimpleDirectory.Entry("blobe", new BaseRef(SHA1E), CcouchNamespace.TT_SHORTHAND_BLOB));
		oldSubDir2.addDirectoryEntry(new SimpleDirectory.Entry("blobf", new BaseRef(SHA1F), CcouchNamespace.TT_SHORTHAND_BLOB));
		oldDir = new SimpleDirectory();
		oldDir.addDirectoryEntry(new SimpleDirectory.Entry("bloba", new BaseRef(SHA1A), CcouchNamespace.TT_SHORTHAND_BLOB));
		oldDir.addDirectoryEntry(new SimpleDirectory.Entry("blobb", new BaseRef(SHA1B), CcouchNamespace.TT_SHORTHAND_BLOB));
		oldDir.addDirectoryEntry(new SimpleDirectory.Entry("subdir1", oldSubDir1, CcouchNamespace.TT_SHORTHAND_DIRECTORY));
		oldDir.addDirectoryEntry(new SimpleDirectory.Entry("subdir2", oldSubDir2, CcouchNamespace.TT_SHORTHAND_DIRECTORY));
		
		SimpleDirectory newSubDir1 = new SimpleDirectory();
		newSubDir1.addDirectoryEntry(new SimpleDirectory.Entry("blobc", new BaseRef(SHA1C), CcouchNamespace.TT_SHORTHAND_BLOB));
		newSubDir1.addDirectoryEntry(new SimpleDirectory.Entry("blobd", new BaseRef(SHA1E), CcouchNamespace.TT_SHORTHAND_BLOB));
		SimpleDirectory newSubDir2 = new SimpleDirectory();
		newSubDir2.addDirectoryEntry(new SimpleDirectory.Entry("blobe", new BaseRef(SHA1E), CcouchNamespace.TT_SHORTHAND_BLOB));
		newSubDir2.addDirectoryEntry(new SimpleDirectory.Entry("blobf", new BaseRef(SHA1F), CcouchNamespace.TT_SHORTHAND_BLOB));
		newDir = new SimpleDirectory();
		newDir.addDirectoryEntry(new SimpleDirectory.Entry("bloba", new BaseRef(SHA1A), CcouchNamespace.TT_SHORTHAND_BLOB));
		newDir.addDirectoryEntry(new SimpleDirectory.Entry("blobb", new BaseRef(SHA1C), CcouchNamespace.TT_SHORTHAND_BLOB));
		newDir.addDirectoryEntry(new SimpleDirectory.Entry("subdir1", newSubDir1, CcouchNamespace.TT_SHORTHAND_DIRECTORY));
		newDir.addDirectoryEntry(new SimpleDirectory.Entry("subdir2a", newSubDir2, CcouchNamespace.TT_SHORTHAND_DIRECTORY));
	}
	
	public void testChangeset() {
		Changeset cs = MergeUtil.getChanges(oldDir, newDir);
		assertEquals(
			"D  blobb\n"+
			"A  blobb\n"+
			"D  subdir1/blobd\n"+
			"A  subdir1/blobd\n"+
			"DD subdir2\n"+
			"D  subdir2/blobe\n"+
			"D  subdir2/blobf\n"+
			"A  subdir2a/blobe\n"+
			"A  subdir2a/blobf\n",
			cs.dump()
		);
	}
}