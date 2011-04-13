package contentcouch.merge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.base.BaseRequest;
import contentcouch.blob.Blob;
import contentcouch.blob.BlobUtil;
import contentcouch.commit.SimpleCommit;
import contentcouch.directory.SimpleDirectory;
import contentcouch.framework.TheGetter;
import contentcouch.misc.MetadataUtil;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.rdf.RdfCommit;
import contentcouch.rdf.RdfDirectory;
import contentcouch.repository.MetaRepoConfig;
import contentcouch.repository.RepoConfig;
import contentcouch.value.BaseRef;
import contentcouch.value.Commit;
import contentcouch.value.Ref;

public class MergeUtilTest extends TestCase
{
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
	
	protected static Blob someBlob = BlobUtil.getBlob("Hello, world!");
	
	MetaRepoConfig mrc;
	RepoConfig testRepoConfig = new RepoConfig(RepoConfig.DISPOSITION_LOCAL, "x-memtemp:/test-repo/", "test-repo");
	public void setUp() {
		mrc = new MetaRepoConfig();
		TheGetter.globalInstance = mrc.getRequestKernel();
		mrc.addRepoConfig(testRepoConfig);
	}
	
	protected Ref storeCommit(Commit c) {
		RdfCommit rdfCommit = new RdfCommit(c, RdfDirectory.DEFAULT_TARGET_RDFIFIER);
		BaseRequest storeCommitReq = TheGetter.createRequest(RequestVerbs.PUT, "x-ccouch-repo://test-repo/data");
		storeCommitReq.content = BlobUtil.getBlob(rdfCommit.toString());
		storeCommitReq.putMetadata(CCouchNamespace.REQ_FILEMERGE_METHOD, CCouchNamespace.REQ_FILEMERGE_STRICTIG);
		Response storeCommitRes = TheGetter.callAndThrowIfNonNormalResponse(storeCommitReq);
		String commitBlobUrn = MetadataUtil.getStoredIdentifier(storeCommitRes);
		return new BaseRef( CCouchNamespace.RDF_SUBJECT_URI_PREFIX + commitBlobUrn );
	}
	
	protected Ref storeCommit( Object target, Object[] parents ) {
		SimpleCommit sc = new SimpleCommit();
		sc.target = target;
		sc.parents = parents;
		return storeCommit(sc);
	}
	
	public void testStoreCommit() {
		SimpleCommit sc = new SimpleCommit();
		assertTrue( storeCommit(sc).getTargetUri().startsWith(CCouchNamespace.RDF_SUBJECT_URI_PREFIX+"urn:") );
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
	
	//// Common ancestor
	
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
	
	//// Removing redundant commit URIs ////
	
	protected void spitSet( Set s ) {
		ArrayList l = new ArrayList(s);
		Collections.sort(l);
		for( Iterator i=l.iterator(); i.hasNext(); ) {
			System.err.println( i.next() );
		}
	}
	
	public void testFilterAncestorCommitUris() {
		HashSet allUris = new HashSet(); 
		
		Ref c0Ref = storeCommit(new BaseRef(SHA1A),null);
		Ref c1Ref = storeCommit(new BaseRef(SHA1B),new Object[]{c0Ref});
		Ref c2Ref = storeCommit(new BaseRef(SHA1C),new Object[]{c0Ref});
		Ref c3Ref = storeCommit(new BaseRef(SHA1D),new Object[]{c1Ref});
		Ref c4Ref = storeCommit(new BaseRef(SHA1E),new Object[]{c2Ref});
		Ref c5Ref = storeCommit(new BaseRef(SHA1F),new Object[]{c1Ref,c2Ref});
		Ref c6Ref = storeCommit(new BaseRef(SHA1G),new Object[]{c3Ref,c4Ref});
		
		allUris.add(c0Ref.getTargetUri());
		allUris.add(c1Ref.getTargetUri());
		allUris.add(c2Ref.getTargetUri());
		allUris.add(c3Ref.getTargetUri());
		allUris.add(c4Ref.getTargetUri());
		allUris.add(c5Ref.getTargetUri());
		allUris.add(c6Ref.getTargetUri());
		
		Set cleanedUp0 = MergeUtil.filterAncestorCommitUris(allUris, 0);
		assertEquals( 7, cleanedUp0.size() );
		assertEquals( allUris, cleanedUp0 );
		
		// Should only need to look one deep since the things we pass in
		// point to eachother...
		Set cleanedUp1 = MergeUtil.filterAncestorCommitUris(allUris, 1);
		assertEquals( 2, cleanedUp1.size() );
		assertTrue( cleanedUp1.contains(c5Ref.getTargetUri()) );
		assertTrue( cleanedUp1.contains(c6Ref.getTargetUri()) );

		// OTOH, if we remove one...
		HashSet someUris = new HashSet(allUris);
		someUris.remove(c3Ref.getTargetUri());
		someUris.remove(c5Ref.getTargetUri());
		Set cleanedUp1x = MergeUtil.filterAncestorCommitUris(someUris, 1);
		assertEquals( 2, cleanedUp1.size() );
		assertTrue( cleanedUp1x.contains(c1Ref.getTargetUri()) );
		assertTrue( cleanedUp1x.contains(c6Ref.getTargetUri()) );

		Set cleanedUp10 = MergeUtil.filterAncestorCommitUris(allUris, 10);
		assertEquals( 2, cleanedUp10.size() );
		/*
		System.err.println("All URIs:");
		spitSet( allUris );
		System.err.println("Cleaned up 10:");
		spitSet( cleanedUp10 );
		*/
		assertTrue( cleanedUp10.contains(c5Ref.getTargetUri()) );
		assertTrue( cleanedUp10.contains(c6Ref.getTargetUri()) );
	}
	
	////
	
	public void testChangesetDump() {
		Changeset cs = new Changeset();
		cs.addChange(new FileAdd("foo",new BaseRef(SHA1A),2345,null));
		cs.addChange(new FileAdd("bar",new BaseRef(SHA1B),2345,new FileDelete("bar",null)));
		assertEquals(
			"D  bar\n"+
			"A  bar\n"+
			"A  foo\n",
			cs.dump()
		);
	}
	
	static SimpleDirectory oldDir;
	static SimpleDirectory oldDir2;
	static SimpleDirectory newDir;
	static {
		SimpleDirectory oldSubDir1 = new SimpleDirectory();
		oldSubDir1.addDirectoryEntry(new SimpleDirectory.Entry("blobc", new BaseRef(SHA1C), CCouchNamespace.TT_SHORTHAND_BLOB));
		oldSubDir1.addDirectoryEntry(new SimpleDirectory.Entry("blobd", new BaseRef(SHA1D), CCouchNamespace.TT_SHORTHAND_BLOB));
		SimpleDirectory oldSubDir2 = new SimpleDirectory();
		oldSubDir2.addDirectoryEntry(new SimpleDirectory.Entry("blobe", new BaseRef(SHA1E), CCouchNamespace.TT_SHORTHAND_BLOB));
		oldSubDir2.addDirectoryEntry(new SimpleDirectory.Entry("blobf", new BaseRef(SHA1F), CCouchNamespace.TT_SHORTHAND_BLOB));
		oldDir = new SimpleDirectory();
		oldDir.addDirectoryEntry(new SimpleDirectory.Entry("bloba", new BaseRef(SHA1A), CCouchNamespace.TT_SHORTHAND_BLOB));
		oldDir.addDirectoryEntry(new SimpleDirectory.Entry("blobb", new BaseRef(SHA1B), CCouchNamespace.TT_SHORTHAND_BLOB));
		oldDir.addDirectoryEntry(new SimpleDirectory.Entry("subdir1", oldSubDir1, CCouchNamespace.TT_SHORTHAND_DIRECTORY));
		oldDir.addDirectoryEntry(new SimpleDirectory.Entry("subdir2", oldSubDir2, CCouchNamespace.TT_SHORTHAND_DIRECTORY));

		SimpleDirectory oldSubDir3 = new SimpleDirectory();
		oldSubDir3.addDirectoryEntry(new SimpleDirectory.Entry("blobc", new BaseRef(SHA1C), CCouchNamespace.TT_SHORTHAND_BLOB));
		oldSubDir3.addDirectoryEntry(new SimpleDirectory.Entry("blobd", new BaseRef(SHA1D), CCouchNamespace.TT_SHORTHAND_BLOB));
		SimpleDirectory oldSubDir4 = new SimpleDirectory();
		oldSubDir4.addDirectoryEntry(new SimpleDirectory.Entry("blobe", new BaseRef(SHA1E), CCouchNamespace.TT_SHORTHAND_BLOB));
		oldSubDir4.addDirectoryEntry(new SimpleDirectory.Entry("blobf", new BaseRef(SHA1F), CCouchNamespace.TT_SHORTHAND_BLOB));
		oldSubDir4.addDirectoryEntry(new SimpleDirectory.Entry("subdir3", oldSubDir3, CCouchNamespace.TT_SHORTHAND_DIRECTORY));
		oldDir2 = new SimpleDirectory();
		oldDir2.addDirectoryEntry(new SimpleDirectory.Entry("bloba", new BaseRef(SHA1A), CCouchNamespace.TT_SHORTHAND_BLOB));
		oldDir2.addDirectoryEntry(new SimpleDirectory.Entry("blobb", new BaseRef(SHA1B), CCouchNamespace.TT_SHORTHAND_BLOB));
		oldDir2.addDirectoryEntry(new SimpleDirectory.Entry("subdir4", oldSubDir4, CCouchNamespace.TT_SHORTHAND_DIRECTORY));

		SimpleDirectory newSubDir1 = new SimpleDirectory();
		newSubDir1.addDirectoryEntry(new SimpleDirectory.Entry("blobc", new BaseRef(SHA1C), CCouchNamespace.TT_SHORTHAND_BLOB));
		newSubDir1.addDirectoryEntry(new SimpleDirectory.Entry("blobd", new BaseRef(SHA1E), CCouchNamespace.TT_SHORTHAND_BLOB));
		SimpleDirectory newSubDir2 = new SimpleDirectory();
		newSubDir2.addDirectoryEntry(new SimpleDirectory.Entry("blobe", new BaseRef(SHA1E), CCouchNamespace.TT_SHORTHAND_BLOB));
		newSubDir2.addDirectoryEntry(new SimpleDirectory.Entry("blobf", new BaseRef(SHA1F), CCouchNamespace.TT_SHORTHAND_BLOB));
		newDir = new SimpleDirectory();
		newDir.addDirectoryEntry(new SimpleDirectory.Entry("bloba", new BaseRef(SHA1A), CCouchNamespace.TT_SHORTHAND_BLOB));
		newDir.addDirectoryEntry(new SimpleDirectory.Entry("blobb", new BaseRef(SHA1C), CCouchNamespace.TT_SHORTHAND_BLOB));
		newDir.addDirectoryEntry(new SimpleDirectory.Entry("subdir1", newSubDir1, CCouchNamespace.TT_SHORTHAND_DIRECTORY));
		newDir.addDirectoryEntry(new SimpleDirectory.Entry("subdir2a", newSubDir2, CCouchNamespace.TT_SHORTHAND_DIRECTORY));
	}
	
	public void testChangeset() {
		Changeset cs = MergeUtil.getChanges(oldDir, newDir);
		assertEquals(
			"D  blobb\n"+
			"A  blobb\n"+
			"D  subdir1/blobd\n"+
			"A  subdir1/blobd\n"+
			"D  subdir2/blobe\n"+
			"D  subdir2/blobf\n"+
			"DD subdir2\n"+
			"A  subdir2a/blobe\n"+
			"A  subdir2a/blobf\n",
			cs.dump()
		);
	}
	
	public void testChangeset2() {
		Changeset cs = MergeUtil.getChanges(oldDir2, newDir);
		assertEquals(
			"D  blobb\n"+
			"A  blobb\n"+
			"A  subdir1/blobc\n"+
			"A  subdir1/blobd\n"+
			"A  subdir2a/blobe\n"+
			"A  subdir2a/blobf\n"+
			"D  subdir4/blobe\n"+
			"D  subdir4/blobf\n"+
			"D  subdir4/subdir3/blobc\n"+
			"D  subdir4/subdir3/blobd\n"+
			"DD subdir4/subdir3\n"+
			"DD subdir4\n",
			cs.dump()
		);
	}
	
	//// Test change applications ////
	
	public void testApplyChanges1() {
		Changeset cs = new Changeset();
		cs.addChange( new FileAdd("bar", someBlob, 1234, null) );
		cs.addChange( new FileDelete("foo", null) );
		
		SimpleDirectory sd = new SimpleDirectory();
		sd.addDirectoryEntry( new SimpleDirectory.Entry("foo", someBlob, CCouchNamespace.TT_SHORTHAND_BLOB, 3456) );
		sd.addDirectoryEntry( new SimpleDirectory.Entry("baz", someBlob, CCouchNamespace.TT_SHORTHAND_BLOB, 3456) );
		
		MergeUtil.applyChanges(sd, cs, Collections.EMPTY_MAP);
		
		assertEquals( 2, sd.getDirectoryEntrySet().size() );
		assertNull( sd.getDirectoryEntry("foo") );
		assertNotNull( sd.getDirectoryEntry("bar") );
		assertNotNull( sd.getDirectoryEntry("baz") );
	}
}
