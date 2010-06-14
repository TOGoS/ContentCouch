package contentcouch.merge;

import contentcouch.contentaddressing.BitprintScheme;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.store.TheGetter;
import contentcouch.value.Commit;
import contentcouch.value.Ref;

public class MergeUtil {
	public static Commit getCommit( String cUrn ) {
		return (Commit)TheGetter.get(cUrn);
	}
	
	public static Commit getCommit( Object c ) {
		if( c instanceof Commit ) return (Commit)c;
		
		if( c instanceof Ref ) return getCommit( ((Ref)c).getTargetUri() );
		
		throw new RuntimeException( "Can't get a commit object based on "+ValueUtil.describe(c) );
	}
	
	/**
	 * 
	 * @param uri1
	 * @param uri2
	 * @return Boolean.TRUE if uris were determined to reference the same object,
	 *   Boolean.FALSE if they were determined to reference different objects,
	 *   null if no determination could be made.
	 */
	public static Boolean areUrisEquivalent( String uri1, String uri2 ) {
		String uri1Parsed = UriUtil.stripRdfSubjectPrefix(uri1);
		String uri2Parsed = UriUtil.stripRdfSubjectPrefix(uri2);
		if( uri1Parsed != null && uri2Parsed != null ) {
			uri1 = uri1Parsed;
			uri2 = uri2Parsed;
		} else if( uri1Parsed != null || uri2Parsed != null ) {
			return null;
		}
		
		Boolean bitprintCompatible = BitprintScheme.Bitprint.getUriEquivalence(uri1, uri2);
		if( bitprintCompatible != null ) {
			return bitprintCompatible;
		}
		
		return null;
	}
	
	public static String findCommonAncestor( String c1Urn, String c2Urn ) {
		if( c1Urn.equals(c2Urn) ) return c1Urn;
		
		// TODO: implement somehow
		return null;
	}
}
