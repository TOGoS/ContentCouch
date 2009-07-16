package contentcouch.directory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import contentcouch.app.Log;
import contentcouch.blob.BlobUtil;
import contentcouch.contentaddressing.BitprintScheme.Bitprint;
import contentcouch.file.FileBlob;
import contentcouch.misc.Function1;
import contentcouch.misc.SimpleDirectory;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.store.TheGetter;
import contentcouch.value.Blob;
import contentcouch.value.Directory;
import contentcouch.value.Ref;
import contentcouch.value.Directory.Entry;

public class DirectoryMerger {
	public static interface ConflictResolver {
		public void resolve( WritableDirectory dir, Directory.Entry e1, Directory.Entry e2, String srcUri, String destUri );
	};
	
	public static class RegularConflictResolver implements ConflictResolver {
		public String dirMergeMethod;
		public String fileMergeMethod;
		
		public RegularConflictResolver() {
			fileMergeMethod = CcouchNamespace.REQ_FILEMERGE_FAIL;
			dirMergeMethod = CcouchNamespace.REQ_DIRMERGE_FAIL;
		}
		
		public RegularConflictResolver( Map options ) {
			if( options != null ) {
				dirMergeMethod = ValueUtil.getString(options.get(CcouchNamespace.REQ_DIRMERGE_METHOD));
				fileMergeMethod = ValueUtil.getString(options.get(CcouchNamespace.REQ_FILEMERGE_METHOD));
			}
			if( fileMergeMethod == null ) fileMergeMethod = CcouchNamespace.REQ_FILEMERGE_FAIL;
			if( dirMergeMethod == null ) dirMergeMethod = CcouchNamespace.REQ_DIRMERGE_FAIL;
		}
		
		// TODO: URI equivalence checker should probably be configurable.
		// For now, just use the static Bitprint functions.
		// Maybe move these functions to a utility class, too.

		protected String getComparableUrn( Blob b, String givenUrn ) {
			if( Bitprint.isBitprintCompatibleUri(givenUrn) ) return givenUrn;
			if( b instanceof FileBlob ) {
				// Then we can ask the repository for the cached identifier...
				String urn = TheGetter.identify("x-ccouch-repo:identify", Collections.EMPTY_MAP);
				if( Bitprint.isBitprintCompatibleUri(urn) ) return urn;
			}
			return null;
		}
		
		protected boolean blobsAreEqual( Blob srcBlob, Blob destBlob, String srcUri, String destUri ) {
			// First, try to compare URNs, which might have been passed in or cached somewhere
			srcUri = getComparableUrn( srcBlob, srcUri );
			destUri = getComparableUrn( srcBlob, destUri );
			if( srcUri != null && destUri != null ) {
				if( srcUri.equals(destUri) ) return true; // save a little bit of parsing time...
				Boolean equivalence = Bitprint.getUriEquivalence(srcUri, destUri);
				if( equivalence != null ) return equivalence.booleanValue();
			}
			
			// This can be really expensive, which is why we tried to avoid it...
			System.err.println("Comparing blobs, oh no!");
			return BlobUtil.blobsEqual(srcBlob, destBlob);
		}
		
		protected void mergeBlob(WritableDirectory dir, Entry e1, Entry e2, String mergeMethod) {
			if( CcouchNamespace.REQ_FILEMERGE_IGNORE.equals(mergeMethod) ) {
				Log.log(Log.EVENT_KEPT, e1.getName());
			} else if( CcouchNamespace.REQ_FILEMERGE_REPLACE.equals(mergeMethod) ) {
				Log.log(Log.EVENT_REPLACED, e1.getName(), e2.getName());
				dir.addDirectoryEntry(e2);
			} else if( CcouchNamespace.REQ_FILEMERGE_FAIL.equals(mergeMethod) ) {
				throw new RuntimeException( "Can't merge blobs " + e2.getName() + " into " + e1.getName() + "; file merge method = Fail" );
			} else {
				throw new RuntimeException( "Can't merge blobs " + e2.getName() + " into " + e1.getName() + "; no merge method given" );
			}
		}
		
		public void resolve(WritableDirectory dir, Entry e1, Entry e2, String srcUri, String destUri ) {
			int e1tt = CloneUtil.getTargetTypeIndex(e1);
			int e2tt = CloneUtil.getTargetTypeIndex(e2);
			if( e1tt == e2tt && e1tt == CloneUtil.CLONE_TARGETTYPE_BLOB ) {
				if( fileMergeMethod.startsWith("Same?") ) {
					Blob b1 = BlobUtil.getBlob(DirectoryUtil.getTargetValue(e1.getTarget()));
					Blob b2 = BlobUtil.getBlob(DirectoryUtil.getTargetValue(e2.getTarget()));
					String[] options = fileMergeMethod.substring(5).split(":");
					if( blobsAreEqual(b1, b2, srcUri, destUri) ) {
						mergeBlob( dir, e1, e2, options[0] );
					} else {
						mergeBlob( dir, e1, e2, options[1] );
					}
				} else {
					mergeBlob( dir, e1, e2, fileMergeMethod );
				}
			} else if( e1tt == e2tt && e1tt == CloneUtil.CLONE_TARGETTYPE_DIR ) {
				if( CcouchNamespace.REQ_DIRMERGE_MERGE.equals(dirMergeMethod) ) {
					Object t = DirectoryUtil.getTargetValue(e1.getTarget());
					if( !(t instanceof WritableDirectory) ) {
						throw new RuntimeException( "Can't merge into " + e1.getName() + "; not a WritableDirectory" );
					}
					Object s = DirectoryUtil.getTargetValue(e2.getTarget());
					if( !(s instanceof Directory) ) {
						throw new RuntimeException( "Can't merge from " + e2.getName() + "; not a Directory" );
					}
					new DirectoryMerger( this, true ).putAll( (WritableDirectory)t, (Directory)s, srcUri, destUri );
				} else if( CcouchNamespace.REQ_DIRMERGE_IGNORE.equals(dirMergeMethod) ) {
				} else if( CcouchNamespace.REQ_DIRMERGE_REPLACE.equals(dirMergeMethod) ) {
					dir.addDirectoryEntry(e2);
				} else if( CcouchNamespace.REQ_DIRMERGE_FAIL.equals(dirMergeMethod) ) {
					throw new RuntimeException( "Can't merge dirs " + e2.getName() + " into " + e1.getName() + "; dir merge method = Fail" );
				} else {
					throw new RuntimeException( "Can't merge dirs " + e2.getName() + " into " + e1.getName() + "; no merge method given" );
				}
			} else {
				throw new RuntimeException( "Can't merge entries " + e1.getName() + " and " + e2.getName() + "; Mismatched types " +
					CloneUtil.getTargetTypeName(e1tt) + " and " + CloneUtil.getTargetTypeName(e2tt) );
			}
		}
	}
	
	/** If true, will copy incoming directories into SimpleDirectories instead of adding the given object directly */
	public boolean copyNewDirectories;
	public ConflictResolver conflictResolver;
	
	public DirectoryMerger( ConflictResolver conflictResolver, boolean copyNewDirectories ) {
		this.conflictResolver = conflictResolver;
		this.copyNewDirectories = copyNewDirectories;
	}

	protected Object deepCloneDirectory( Object target ) {
		if( target instanceof Ref ) target = TheGetter.get(((Ref)target).getTargetUri());
		if( !(target instanceof Directory) ) {
			throw new RuntimeException( "Target was expected to be a Directory, but is " + (target == null ? "null" : "a " + target.getClass().getName()));
		}
		return new SimpleDirectory( (Directory)target,
			new Function1() { public Object apply(Object input) {  return deepCloneDirectory(input);  }	},
			new Function1() { public Object apply(Object input) {  return input;  }	}
		);
	}
	
	/** Returns true if something already existed at the destination (whether it was overwritten or not) */
	public boolean put( WritableDirectory dir, Directory.Entry newEntry, String srcUri, String destUri ) {
		Directory.Entry existingEntry = dir.getDirectoryEntry(newEntry.getName());
		if( existingEntry != null ) {
			if( conflictResolver == null ) {
				throw new RuntimeException("Cannot resolve merge conflict on " + newEntry.getName() + "; no clonflict resolver supploed");
			} else {
				conflictResolver.resolve( dir, existingEntry, newEntry, srcUri, destUri );
			}
			return true;
		} else {
			Log.log(Log.EVENT_PUT, srcUri, destUri );
			if( copyNewDirectories && CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(newEntry.getTargetType()) ) {
				Object target = newEntry.getTarget();
				SimpleDirectory.Entry simpleEntry = new SimpleDirectory.Entry( newEntry );
				simpleEntry.target = deepCloneDirectory(target);
				newEntry = simpleEntry;
			}
			dir.addDirectoryEntry(newEntry);
			return false;
		}
	}
	
	public void putAll( WritableDirectory destDir, Directory srcDir, String srcUri, String destUri ) {
		for( Iterator i=srcDir.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry e = (Directory.Entry)i.next();
			put( destDir, e, PathUtil.appendPath(srcUri, e.getName(), false), PathUtil.appendPath(destUri, e.getName(), false) );
		}
	}
}
