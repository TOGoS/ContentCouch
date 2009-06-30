package contentcouch.repository;

import contentcouch.repository.MetaRepository.RepoRef;
import junit.framework.TestCase;

public class RepoRefTest extends TestCase {
	public void testParseCompleteRepoRef() {
		RepoRef rr;
		
		rr = RepoRef.parse("//xyz/123/aba", false);
		assertEquals( "xyz", rr.repoName );
		assertEquals( "123/aba", rr.subPath );

		rr = RepoRef.parse("//xyz/123/aba", true);
		assertEquals( "xyz", rr.repoName );
		assertEquals( "heads/123/aba", rr.subPath );

		rr = RepoRef.parse("x-ccouch-repo://xyz/123/aba", false);
		assertEquals( "xyz", rr.repoName );
		assertEquals( "123/aba", rr.subPath );

		rr = RepoRef.parse("x-ccouch-repo://xyz/123/aba", true);
		assertEquals( "xyz", rr.repoName );
		assertEquals( "123/aba", rr.subPath );
	
		rr = RepoRef.parse("x-ccouch-head://xyz/123/aba", false);
		assertEquals( "xyz", rr.repoName );
		assertEquals( "heads/123/aba", rr.subPath );

		rr = RepoRef.parse("x-ccouch-head://xyz/123/aba", true);
		assertEquals( "xyz", rr.repoName );
		assertEquals( "heads/123/aba", rr.subPath );
	}
		
	public void testParseIncompleteRepoRef() {
		RepoRef rr;
		
		rr = RepoRef.parse("123/aba", true);
		assertEquals( null, rr.repoName );
		assertEquals( "heads/123/aba", rr.subPath );
		assertNotSame( null, rr.getHeadPath() );

		rr = RepoRef.parse("123/aba", false);
		assertEquals( null, rr.repoName );
		assertEquals( "123/aba", rr.subPath );
		assertEquals( null, rr.getHeadPath() );
	}
}
