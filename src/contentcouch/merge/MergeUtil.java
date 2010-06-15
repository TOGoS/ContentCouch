package contentcouch.merge;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import contentcouch.contentaddressing.BitprintScheme;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.store.TheGetter;
import contentcouch.value.Commit;
import contentcouch.value.Directory;
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
	
	protected static class AncestorFinder {
		Map commits = new HashMap();
		Map commitParents = new HashMap();
		
		protected Commit getCommit( String urn ) {
			if( commits.containsKey(urn) ) {
				return (Commit)commits.get(urn);
			}
			Commit c = MergeUtil.getCommit(urn);
			commits.put(urn,c);
			return c;
		}
		
		public Set getParentCommitUris( String commitUri ) {
			Commit c = getCommit(commitUri);
			if( c != null ) {
				Set parentUris = new HashSet();
				Object[] parents = c.getParents();
				for( int i=parents.length-1; i>=0; --i ) {
					if( parents[i] instanceof Ref ) {
						parentUris.add( ((Ref)parents[i]).getTargetUri() );
					}
				}
				return parentUris;
			}
			return Collections.EMPTY_SET;
		}
		
		public Set getParentCommitUris( Set commitUris ) {
			Set byGum = new HashSet();
			for( Iterator i=commitUris.iterator(); i.hasNext(); ) {
				byGum.addAll(getParentCommitUris((String)i.next()));
			}
			return byGum;
		}
		
		public String findCommonAncestor( String c1Urn, String c2Urn ) {
			if( c1Urn.equals(c2Urn) ) return c1Urn;
			
			Set newParents1 = getParentCommitUris(c1Urn);
			Set newParents2 = getParentCommitUris(c2Urn);
			Set ancestors1 = new HashSet();
			ancestors1.add(c1Urn);
			Set ancestors2 = new HashSet();
			ancestors2.add(c2Urn);
			
			while( newParents1.size() > 0 || newParents2.size() > 0 ) {
				if( newParents1.size() > 0 ) {
					for( Iterator i=newParents1.iterator(); i.hasNext(); ) {
						String curi = (String)i.next();
						if( ancestors2.contains(curi) ) {
							return curi;
						}
					}
					ancestors1.addAll(newParents1);
					newParents1 = getParentCommitUris(newParents1);
				}
				if( newParents2.size() > 0 ) {
					for( Iterator i=newParents2.iterator(); i.hasNext(); ) {
						String curi = (String)i.next();
						if( ancestors1.contains(curi) ) {
							return curi;
						}
					}
					ancestors2.addAll(newParents2);
					newParents2 = getParentCommitUris(newParents2);
				}
			}
			return null;
		}
	}
	
	public static String findCommonAncestor( String c1Urn, String c2Urn ) {
		return new AncestorFinder().findCommonAncestor(c1Urn, c2Urn);
	}
	
	//// Find changes between 2 trees ////
	
	protected static String appendPath( String oldPath, String entryName ) {
		if( oldPath == null || oldPath.length() == 0 ) return entryName;
		return oldPath+"/"+entryName;
	}
	
	protected static void addChanges( Changeset cs, String path, Directory oldDir, Directory newDir ) {
		Set visited = new HashSet();
		if( oldDir != null ) for( Iterator oldEntries = oldDir.getDirectoryEntrySet().iterator(); oldEntries.hasNext(); ) {
			Directory.Entry de = (Directory.Entry)oldEntries.next();
			addChanges( cs, appendPath(path,de.getName()), de,
				newDir == null ? null : newDir.getDirectoryEntry(de.getName()));
			visited.add( de.getName() );
		}
		if( newDir != null ) for( Iterator newEntries = newDir.getDirectoryEntrySet().iterator(); newEntries.hasNext(); ) {
			Directory.Entry de = (Directory.Entry)newEntries.next();
			if( visited.contains(de.getName()) ) continue; // Don't process them twice!
			addChanges( cs, appendPath(path,de.getName()),
				oldDir == null ? null : oldDir.getDirectoryEntry(de.getName()), de);
		}
	}
	
	protected static void addChanges( Changeset cs, String path, Object oldObj, boolean oldObjIsDir, Object newObj, boolean newObjIsDir ) {
		Boolean equiv;
		
		if( oldObj == newObj ) {
			return;
		}
		
		if( oldObj instanceof Ref && newObj instanceof Ref &&
			(equiv = areUrisEquivalent(((Ref)oldObj).getTargetUri(), ((Ref)newObj).getTargetUri())) != null &&
			equiv.booleanValue() )
		{
			return;
		}
		
		FileChange c = null;
		Directory oldDir = null;
		if( oldObjIsDir ) {
			oldDir = (Directory)TheGetter.dereference(oldObj);
			if( !newObjIsDir || newObj == null ) {
				cs.addChange(c = new DirDelete(path, c));
				addChanges(cs, path, oldDir, null);
			}
		} else if( oldObj != null ) {
			cs.addChange(c = new FileDelete(path,c));
		}
		
		if( newObjIsDir ) {
			Directory newDir = (Directory)TheGetter.dereference(newObj);
			if( oldObjIsDir ) { 
				oldDir = (Directory)TheGetter.dereference(oldObj);
			}
			addChanges( cs, path, oldDir, newDir );
		} else if( newObj != null ) {
			cs.addChange( new FileAdd(path, newObj, c) );
		}
	}
	
	protected static void addChanges( Changeset cs, String path, Directory.Entry oldEntry, Directory.Entry newEntry ) {
		addChanges( cs, path,
			oldEntry != null ? oldEntry.getTarget() : null,
			oldEntry != null ? CcouchNamespace.TT_SHORTHAND_DIRECTORY.equals(oldEntry.getTargetType()) : false,
			newEntry != null ? newEntry.getTarget() : null,
			newEntry != null ? CcouchNamespace.TT_SHORTHAND_DIRECTORY.equals(newEntry.getTargetType()) : false
		);
	}
	
	protected static Object dereference( Object o ) {
		o = TheGetter.dereference(o);
		while( o instanceof Commit ) {
			o = TheGetter.dereference(((Commit)o).getTarget());
		}
		return o;
	}
	
	public static Changeset getChanges( Object oldObj, Object newObj ) {
		Changeset cs = new Changeset();
		oldObj = dereference(oldObj);
		newObj = dereference(newObj);
		addChanges( cs, "", oldObj, oldObj instanceof Directory, newObj, newObj instanceof Directory );
		return cs;
	}
}