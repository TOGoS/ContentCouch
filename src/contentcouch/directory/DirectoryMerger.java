package contentcouch.directory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import contentcouch.app.Log;
import contentcouch.blob.Blob;
import contentcouch.blob.BlobUtil;
import contentcouch.contentaddressing.BitprintScheme.Bitprint;
import contentcouch.file.FileBlob;
import contentcouch.framework.TheGetter;
import contentcouch.misc.Function1;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.value.Directory;
import contentcouch.value.Directory.Entry;
import contentcouch.value.Ref;

public class DirectoryMerger {
	protected static class EqualityCheckResult {
		public boolean equals;
		public String reason;
		public EqualityCheckResult( boolean equals, String reason ) {
			this.equals = equals;
			this.reason = reason;
		}
	}
	
	public static interface ConflictResolver {
		/**
		 * Resolves differences (possibly by throwing an error)
		 * between two directory entries and writes the result to dir
		 * at entry1's name (entry2's name is ignored)
		 * @param dir
		 * @param entry1 the existing entry
		 * @param entry1Uri URI of the existing entry
		 * @param entry2 the new entry to be merged into the existing one
		 * @param entry2Uri URI of the new entry
		 */
		public void resolve(
			WritableDirectory dir,
			Directory.Entry entry1,
			String entry1Uri,
			Directory.Entry entry2,
			String entry2Uri
		);
	};
	
	public static class RegularConflictResolver implements ConflictResolver {
		protected Map options;
		public String dirMergeMethod;
		public String fileMergeMethod;
		
		public RegularConflictResolver() {
			fileMergeMethod = CCouchNamespace.REQ_FILEMERGE_FAIL;
			dirMergeMethod = CCouchNamespace.REQ_DIRMERGE_FAIL;
		}
		
		public RegularConflictResolver( Map options ) {
			this.options = options;
			if( options != null ) {
				dirMergeMethod = ValueUtil.getString(options.get(CCouchNamespace.REQ_DIRMERGE_METHOD));
				fileMergeMethod = ValueUtil.getString(options.get(CCouchNamespace.REQ_FILEMERGE_METHOD));
			}
			if( fileMergeMethod == null ) fileMergeMethod = CCouchNamespace.REQ_FILEMERGE_FAIL;
			if( dirMergeMethod == null ) dirMergeMethod = CCouchNamespace.REQ_DIRMERGE_FAIL;
		}
		
		// TODO: URI equivalence checker should probably be configurable.
		// For now, just use the static Bitprint functions.
		// Maybe move these functions to a utility class, too.

		protected String getComparableUrn( Blob b, String givenUrn ) {
			if( Bitprint.isBitprintCompatibleUri(givenUrn) ) return givenUrn;
			if( b instanceof FileBlob ) {
				// Then we can ask the repository for the cached identifier...
				String urn = TheGetter.identify(b, Collections.EMPTY_MAP, options);
				if( Bitprint.isBitprintCompatibleUri(urn) ) return urn;
			}
			return null;
		}

		protected String getBlobDescription( Blob b ) {
			if( b instanceof FileBlob ) { return "FileBlob<" + ((FileBlob)b).getPath() + ">"; }
			return b.getClass().getName();
		}
		
		protected EqualityCheckResult blobsAreEqual( Blob srcBlob, Blob destBlob, String srcUri, String destUri ) {
			// First, try to compare URNs, which might have been passed in or cached somewhere
			srcUri = getComparableUrn( srcBlob, srcUri );
			destUri = getComparableUrn( destBlob, destUri );
			if( srcUri != null && destUri != null ) {
				if( srcUri.equals(destUri) ) return new EqualityCheckResult(true,"URI equality"); // save a little bit of parsing time...
				
				Boolean equivalence = Bitprint.getUriEquivalence(srcUri, destUri);
				if( equivalence != null ) return new EqualityCheckResult(equivalence.booleanValue(),"URI equivalence ("+srcUri+", "+destUri+")");
			}
			
			// This can be really expensive, which is why we tried to avoid it...
			Log.log( Log.EVENT_PERFORMANCE_WARNING, "Comparing blobs byte-by-byte: " + getBlobDescription(srcBlob) + ", " + getBlobDescription(destBlob) );
			return new EqualityCheckResult(BlobUtil.blobsEqual(srcBlob, destBlob), "byte-wise check");
		}
		
		protected void mergeBlob(WritableDirectory dir, Entry e1, Entry e2, String mergeMethod, String context) {
			if( CCouchNamespace.REQ_FILEMERGE_IGNORE.equals(mergeMethod) ) {
				Log.log(Log.EVENT_KEPT, e1.getName());
			} else if( CCouchNamespace.REQ_FILEMERGE_REPLACE.equals(mergeMethod) ) {
				Log.log(Log.EVENT_REPLACED, e1.getName(), e2.getName());
				dir.addDirectoryEntry(e2, options);
			} else if( CCouchNamespace.REQ_FILEMERGE_FAIL.equals(mergeMethod) ) {
				throw new RuntimeException( "Can't merge blobs " + e2.getName() + " into " + e1.getName() + "; "+context );
			} else {
				throw new RuntimeException( "Can't merge blobs " + e2.getName() + " into " + e1.getName() + "; no merge method given" );
			}
		}
		
		public void resolve(WritableDirectory dir, Entry e1, String e1Uri, Entry e2, String e2Uri ) {
			int e1tt = CloneUtil.getTargetTypeIndex(e1);
			int e2tt = CloneUtil.getTargetTypeIndex(e2);
			if( e1tt == e2tt && e1tt == CloneUtil.CLONE_TARGETTYPE_BLOB ) {
				if( fileMergeMethod.startsWith("Same?") ) {
					Blob b1 = BlobUtil.getBlob(DirectoryUtil.resolveTarget(e1));
					Blob b2 = BlobUtil.getBlob(DirectoryUtil.resolveTarget(e2));
					String[] options = fileMergeMethod.substring(5).split(":");
					EqualityCheckResult blobEquality = blobsAreEqual(b1, b2, e2Uri, e1Uri);
					if( blobEquality.equals ) {
						mergeBlob( dir, e1, e2, options[0], "merge method = " + fileMergeMethod );
					} else {
						mergeBlob( dir, e1, e2, options[1], "merge method = " + fileMergeMethod + ", checked by " + blobEquality.reason );
					}
				} else {
					mergeBlob( dir, e1, e2, fileMergeMethod, "merge method = " + fileMergeMethod );
				}
			} else if( e1tt == e2tt && e1tt == CloneUtil.CLONE_TARGETTYPE_DIR ) {
				if( CCouchNamespace.REQ_DIRMERGE_MERGE.equals(dirMergeMethod) ) {
					Object t = DirectoryUtil.resolveTarget(e1);
					if( !(t instanceof WritableDirectory) ) {
						throw new RuntimeException( "Can't merge into " + e1.getName() + " ("+e1Uri+"); not a WritableDirectory" );
					}
					Object s = DirectoryUtil.resolveTarget(e2);
					if( !(s instanceof Directory) ) {
						throw new RuntimeException( "Can't merge from " + e2.getName() + " ("+e2Uri+"); not a Directory" );
					}
					new DirectoryMerger( this, options ).putAll( (WritableDirectory)t, (Directory)s, e2Uri, e1Uri );
				} else if( CCouchNamespace.REQ_DIRMERGE_IGNORE.equals(dirMergeMethod) ) {
				} else if( CCouchNamespace.REQ_DIRMERGE_REPLACE.equals(dirMergeMethod) ) {
					dir.addDirectoryEntry(e2, options);
				} else if( CCouchNamespace.REQ_DIRMERGE_FAIL.equals(dirMergeMethod) ) {
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
	protected final boolean copyNewDirectories;
	protected final ConflictResolver conflictResolver;
	protected final Map options;
	
	public DirectoryMerger( ConflictResolver conflictResolver, Map options ) {
		if( conflictResolver == null ) throw new RuntimeException("Didn't pass a conflict resolver to DirectoryMerger constructor!");
		if( options == null ) throw new RuntimeException("Didn't pass options to DirectoryMerger constructor!");
		
		this.conflictResolver = conflictResolver;
		this.copyNewDirectories = MetadataUtil.isEntryTrue(options, CCouchNamespace.REQ_COPY_SOURCE_DIRS);
		this.options = options;
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
	
	/**
	 * Returns true if something already existed at the destination (whether it was overwritten or not)
	 * @param WritableDirectory dir directory into which the new object will be put
	 * @param Directory.Entry newEntry the entry to be added (or merged into an existing one) within dir
	 * @param String srcUri URI of newEntry
	 * @param String destUri URI of the entry that will be written to dir
	 * */
	public boolean put( WritableDirectory dir, Directory.Entry newEntry, String srcUri, String destUri ) {
		Directory.Entry existingEntry = dir.getDirectoryEntry(newEntry.getName());
		if( existingEntry != null ) {
			if( conflictResolver == null ) {
				throw new RuntimeException("Cannot resolve merge conflict on " + newEntry.getName() + "; no clonflict resolver supploed");
			} else {
				conflictResolver.resolve( dir, existingEntry, destUri, newEntry, srcUri );
			}
			return true;
		} else {
			Log.log(Log.EVENT_PUT, srcUri, destUri );
			if( copyNewDirectories && CCouchNamespace.TT_SHORTHAND_DIRECTORY.equals(newEntry.getTargetType()) ) {
				Object target = newEntry.getTarget();
				SimpleDirectory.Entry simpleEntry = new SimpleDirectory.Entry( newEntry );
				simpleEntry.target = deepCloneDirectory(target);
				newEntry = simpleEntry;
			}
			dir.addDirectoryEntry(newEntry, options);
			return false;
		}
	}
	
	public void putAll( WritableDirectory destDir, Directory srcDir, String srcUri, String destUri ) {
		if( MetadataUtil.isEntryTrue(options, CCouchNamespace.REQ_USE_URI_DOT_FILES) ) {
			Directory.Entry uriDotFileEntry = destDir.getDirectoryEntry(".ccouch-uri");
			if( uriDotFileEntry != null ) {
				Object target = uriDotFileEntry.getTarget();
				if( target instanceof Ref ) target = TheGetter.get( ((Ref)target).getTargetUri() );
				String destCurrentUrn = ValueUtil.getString(target);
				if( srcUri.equals(destCurrentUrn) ) {
					// System.err.println(destUri+" already = "+srcUri+"; skipping recursive dir merge!");
					return;
				}
			}
		}
		
		for( Iterator i=srcDir.getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry e = (Directory.Entry)i.next();
			String sourceUri;
			if( e.getTarget() instanceof Ref ) {
				sourceUri = ((Ref)e.getTarget()).getTargetUri();
			} else {
				sourceUri = PathUtil.appendPath(srcUri, e.getName(), false);
			}
			put( destDir, e, sourceUri, PathUtil.appendPath(destUri, e.getName(), false) );
		}
	}
}
