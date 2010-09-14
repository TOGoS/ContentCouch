package contentcouch.merge;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;

import contentcouch.app.Log;
import contentcouch.contentaddressing.BitprintScheme;
import contentcouch.context.Context;
import contentcouch.directory.SimpleDirectory;
import contentcouch.directory.WritableDirectory;
import contentcouch.framework.TheGetter;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.value.Commit;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class MergeUtil {
	public static Commit getCommit( String cUrn ) {
		BaseRequest req = new BaseRequest(RequestVerbs.VERB_GET, cUrn);
		req.metadata = new HashMap(Context.getInstance());
		if( !req.metadata.containsKey(CCouchNamespace.REQ_CACHE_SECTOR) ) {
			req.metadata.put(CCouchNamespace.REQ_CACHE_SECTOR, "remote");
		}
		Response res = TheGetter.call(req);
		switch( res.getStatus() ) {
		case( ResponseCodes.RESPONSE_UNHANDLED ):
		case( ResponseCodes.RESPONSE_DOESNOTEXIST ):
			return null;
		default:
			return (Commit)TheGetter.getResponseValue( res, req );
		}
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
	
	//// Filter out ancestor commit URIs ////
	
	static class AncestorCleaner {
		public AncestorCleaner( Map options ) {
			// :/  ?
		}
		
		protected Commit getCommit( String commitUri ) {
			Commit c = MergeUtil.getCommit(commitUri);
			if( c == null ) {
				Log.log(Log.EVENT_WARNING, "Couldn't find commit "+commitUri);
			}
			return c;
		}
		
		protected void collectAncestors( String commitUri, int height, Map alreadyHit ) {
			Integer alreadyHitHeight = (Integer)alreadyHit.get(commitUri);
			if( alreadyHitHeight == null || alreadyHitHeight.intValue() > height ) {
				alreadyHit.put(commitUri,new Integer(height));
			}

			if( height > 0 ) {
				Commit c = getCommit(commitUri);
				if( c == null ) return;
				Object[] p = c.getParents();
				for( int i=0; i<p.length; ++i ) {
					collectAncestors( ((Ref)p[i]).getTargetUri(), height-1, alreadyHit );
				}
			}
		}
		
		public Set filterAncestorCommitUris( Collection commitUris, int depth ) {
			Map ahDepth = new HashMap();
			for( Iterator i=commitUris.iterator(); i.hasNext(); ) {
				collectAncestors( (String)i.next(), depth, ahDepth );
			}
			HashSet naCommitUris = new HashSet();
			for( Iterator i = ahDepth.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry e = (Map.Entry)i.next();
				if( ((Integer)e.getValue()).intValue() == depth ) {
					naCommitUris.add( e.getKey() );
				}
			}
			return naCommitUris;
		}
	}
	
	public static Set filterAncestorCommitUris( Collection commitUris, int depth ) {
		return new AncestorCleaner( Context.getInstance() ).filterAncestorCommitUris( commitUris, depth );
	}
	
	//// Find changes between 2 trees ////
	
	protected static String appendPath( String oldPath, String entryName ) {
		if( oldPath == null || oldPath.length() == 0 ) return entryName;
		return oldPath+"/"+entryName;
	}
	
	protected static void collectChanges( Changeset cs, String path, Directory oldDir, Directory newDir ) {
		Set visited = new HashSet();
		if( oldDir != null ) for( Iterator oldEntries = oldDir.getDirectoryEntrySet().iterator(); oldEntries.hasNext(); ) {
			Directory.Entry de = (Directory.Entry)oldEntries.next();
			collectChanges( cs, appendPath(path,de.getName()), de,
				newDir == null ? null : newDir.getDirectoryEntry(de.getName()));
			visited.add( de.getName() );
		}
		if( newDir != null ) for( Iterator newEntries = newDir.getDirectoryEntrySet().iterator(); newEntries.hasNext(); ) {
			Directory.Entry de = (Directory.Entry)newEntries.next();
			if( visited.contains(de.getName()) ) continue; // Don't process them twice!
			collectChanges( cs, appendPath(path,de.getName()),
				oldDir == null ? null : oldDir.getDirectoryEntry(de.getName()), de);
		}
	}
	
	protected static void collectChanges( Changeset cs, String path, Directory.Entry oldEntry, Directory.Entry newEntry ) {
		Object oldObj = oldEntry != null ? oldEntry.getTarget() : null;
		Object newObj = newEntry != null ? newEntry.getTarget() : null;
		boolean oldObjIsDir = oldEntry != null ? CCouchNamespace.TT_SHORTHAND_DIRECTORY.equals(oldEntry.getTargetType()) : false;
		boolean newObjIsDir = newEntry != null ? CCouchNamespace.TT_SHORTHAND_DIRECTORY.equals(newEntry.getTargetType()) : false;
		
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
				collectChanges(cs, path, oldDir, null);
			}
		} else if( oldObj != null ) {
			cs.addChange(c = new FileDelete(path,c));
		}
		
		if( newObjIsDir ) {
			Directory newDir = (Directory)TheGetter.dereference(newObj);
			if( oldObjIsDir ) { 
				oldDir = (Directory)TheGetter.dereference(oldObj);
			}
			collectChanges( cs, path, oldDir, newDir );
		} else if( newObj != null ) {
			cs.addChange( new FileAdd(path, newObj, newEntry.getTargetType(), newEntry.getLastModified(), c) );
		}
	}
	
	protected static Object dereference( Object o ) {
		o = TheGetter.dereference(o);
		while( o instanceof Commit ) {
			o = TheGetter.dereference(((Commit)o).getTarget());
		}
		return o;
	}
	
	protected static Directory.Entry createDirectoryEntry(Object obj) {
		if( obj instanceof Directory ) {
			return new SimpleDirectory.Entry("(mergeroot)",obj,CCouchNamespace.TT_SHORTHAND_DIRECTORY);
		} else {
			throw new RuntimeException("Cannot directly merge blobs - can only merge directories");
		}
	}
	
	public static Changeset getChanges( Object oldObj, Object newObj ) {
		Changeset cs = new Changeset();
		oldObj = dereference(oldObj);
		newObj = dereference(newObj);
		collectChanges( cs, "", createDirectoryEntry(oldObj), createDirectoryEntry(newObj) );
		return cs;
	}
	
	//// Apply changes ////
	
	protected static void applyChange( WritableDirectory wd, FileChange c, String path, Map options ) {
		int sidx = path.indexOf('/');
		if( sidx != -1 ) {
			String subdirName = path.substring(0,sidx);
			String restOfPath = path.substring(sidx+1);
			Directory.Entry e = wd.getDirectoryEntry(subdirName);
			if( e == null ) {
				if( c instanceof FileDelete || c instanceof DirDelete ) {
					return; // Don't care!
				} else {
					WritableDirectory subdir = new SimpleDirectory();
					Directory.Entry subdirEntry = new SimpleDirectory.Entry(subdirName, subdir, CCouchNamespace.TT_SHORTHAND_DIRECTORY);
					wd.addDirectoryEntry(subdirEntry, options);
					
					subdirEntry = wd.getDirectoryEntry(subdirName);
					subdir = (WritableDirectory)subdirEntry.getTarget();
					applyChange(subdir, c, restOfPath, options );
				}
			} else if( !CCouchNamespace.TT_SHORTHAND_DIRECTORY.equals(e.getTargetType()) ) {
				throw new RuntimeException( "Can't merge '"+path+"' changes into '"+subdirName+"'; "+subdirName+" is not a directory");
			} else if( !(e.getTarget() instanceof WritableDirectory) ) {
				throw new RuntimeException( "Can't merge '"+c.getPath()+"' changes into '"+subdirName+"'; "+subdirName+" is not writable");
			} else {
				WritableDirectory subdir = (WritableDirectory)e.getTarget();
				applyChange( subdir, c, restOfPath, options );
			}
		} else {
			Directory.Entry e = wd.getDirectoryEntry(path);
			if( e == null && (c instanceof FileDelete || c instanceof DirDelete) ) {
				return; // Don't care!
			} else if( c instanceof FileDelete ) {
				if( !CCouchNamespace.TT_SHORTHAND_BLOB.equals(e.getTargetType()) ) {
					throw new RuntimeException( "Can't delete file "+c.getPath()+" - target is not a regular file" );
				}
				wd.deleteDirectoryEntry(path, options);
			} else if( c instanceof DirDelete ) {
				if( !CCouchNamespace.TT_SHORTHAND_BLOB.equals(e.getTargetType()) ) {
					throw new RuntimeException( "Can't delete directory "+c.getPath()+" - target is not a directory" );
				}
				Directory subdir = (Directory)e.getTarget();
				if( subdir.getDirectoryEntrySet().size() > 0 ) {
					// Skip deletion of empty directories
					// TODO: make this optional
					return; 
				}
				wd.deleteDirectoryEntry(path, options);
			} else if( c instanceof FileAdd ) {
				if( e != null ) {
					throw new RuntimeException("Can't add "+c.getPath()+" - target already exists");
				}
				FileAdd fa = (FileAdd)c;
				SimpleDirectory.Entry newEntry = new SimpleDirectory.Entry( path, fa.getTarget(), fa.getTargetType(), fa.getLastModified() );
				wd.addDirectoryEntry(newEntry, options);
			} else {
				throw new RuntimeException("Don't know how to apply change of type "+c.getClass().getName());
			}
		}
	}
	
	/**
	 * @param wd writable directory to apply changes to
	 * @param cs the changeset containing changes to be applied
	 * @param options fully namespaced option values
	 */
	public static void applyChanges( WritableDirectory wd, Changeset cs, Map options ) {
		for( Iterator i=cs.getOrderedChanges().iterator(); i.hasNext(); ) {
			FileChange fc = (FileChange)i.next();
			applyChange( wd, fc, fc.getPath(), options );
		}
	}
}
