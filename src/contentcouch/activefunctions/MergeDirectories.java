package contentcouch.activefunctions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;

import contentcouch.active.BaseActiveFunction;
import contentcouch.blob.BlobUtil;
import contentcouch.digest.DigestUtil;
import contentcouch.misc.SimpleDirectory;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Directory;

public class MergeDirectories extends BaseActiveFunction {
	protected static final int MERGE_FILE_STRICT  = 0x00; // Source and dest must be strictly mergeable
	protected static final int MERGE_FILE_IGNORE  = 0x01; // When there is a merge conflict, take the existing directory entry
	protected static final int MERGE_FILE_REPLACE = 0x02; // When there is a merge conflict, take the incoming directory entry
	protected static final int MERGE_FILE_MASK    = 0x03;
	
	protected static final int MERGE_MISMATCH_FAIL    = 0x00;
	protected static final int MERGE_MISMATCH_REPLACE = 0x04;
	protected static final int MERGE_MISMATCH_IGNORE  = 0x08;
	protected static final int MERGE_MISMATCH_MASK    = 0x0B;
	
	//protected static final int MERGE_DIR_STRICT  = 0x00;
	protected static final int MERGE_DIR_IGNORE  = 0x10;
	protected static final int MERGE_DIR_REPLACE = 0x20;
	protected static final int MERGE_DIR_MERGE   = 0x40;
	protected static final int MERGE_DIR_MASK    = 0x70;
	
	protected static final int MERGE_FILE_REPLACEONSAME = 0xF0;
	
	protected static String identify( Object o ) {
		return DigestUtil.getSha1Urn(BlobUtil.getBlob(o));
	}
	
	protected static void mergeIntoEntry( SimpleDirectory.Entry destEntry, Directory.Entry srcEntry, int flags ) {
		if( destEntry.target == null ) {
			SimpleDirectory.cloneInto( destEntry, srcEntry, SimpleDirectory.DEEPCLONE_SIMPLEDIRECTORY );
			return;
		}
		
		if( CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(destEntry.getTargetType()) &&
		    CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(srcEntry.getTargetType())
		) {
			// Both Directories
			switch( flags & MERGE_DIR_MASK ) {
			case( MERGE_DIR_IGNORE ):
				break;
			case( MERGE_DIR_REPLACE ):
				destEntry.target = srcEntry.getValue();
				break;
			case( MERGE_DIR_MERGE ):
				if( !(destEntry.target instanceof SimpleDirectory) ) {
					// If it's a SimpleDirectory, it's already been cloned.
					destEntry.target = SimpleDirectory.cloneTarget( destEntry.target, SimpleDirectory.DEEPCLONE_SIMPLEDIRECTORY );
				}
				mergeInto( (SimpleDirectory)destEntry.target, (Directory)srcEntry.getValue(), flags );
			default:
				throw new RuntimeException("Invalid dir merge type: " + (flags & MERGE_DIR_MASK) );
			}
		} else if(
			CcouchNamespace.OBJECT_TYPE_BLOB.equals(destEntry.getTargetType()) &&
			CcouchNamespace.OBJECT_TYPE_BLOB.equals(srcEntry.getTargetType())
		) {
			// Both Blobs
			switch( flags & MERGE_FILE_MASK ) {
			case( MERGE_FILE_STRICT ):
				String srcId = identify(srcEntry.getValue());
				String destId = identify(destEntry.getValue());
				if( srcId == null || destId == null ) {
					throw new RuntimeException("Could not identify object for strict merge");
				}
				if( !srcId.equals(destId) ) {
					throw new RuntimeException("Cannot merge strictly. " + destId + " != " + srcId);
				}
				SimpleDirectory.cloneMetadataInto( destEntry, srcEntry );
				break;
			case( MERGE_FILE_IGNORE ):
				break;
			case( MERGE_FILE_REPLACE ):
				destEntry.target = SimpleDirectory.cloneTarget(srcEntry.getValue(), SimpleDirectory.DEEPCLONE_SIMPLEDIRECTORY);
				SimpleDirectory.cloneMetadataInto( destEntry, srcEntry );
				break;
			default:
				throw new RuntimeException("Invalid blob merge type: " + (flags & MERGE_FILE_MASK) );
			}
		} else {
			// Type mismatch
			switch( flags & MERGE_MISMATCH_MASK ) {
			case( MERGE_MISMATCH_FAIL ):
				String srcId = identify(srcEntry.getValue());
				String destId = identify(destEntry.getValue());
				if( srcId == null || destId == null ) {
					throw new RuntimeException("Could not identify object for strict merge (type check already failed)");
				}
				if( !srcId.equals(destId) ) {
					throw new RuntimeException("Cannot merge strictly.  Type check failed; " + destId + " != " + srcId);
				}
				SimpleDirectory.cloneMetadataInto( destEntry, srcEntry );
				break;
			case( MERGE_MISMATCH_IGNORE ):
				break;
			case( MERGE_MISMATCH_REPLACE ):
				destEntry.target = SimpleDirectory.cloneTarget(srcEntry.getValue(), SimpleDirectory.DEEPCLONE_SIMPLEDIRECTORY);
				SimpleDirectory.cloneMetadataInto( destEntry, srcEntry );
				break;
			default:
				throw new RuntimeException("Invalid mismatch merge type: " + (flags & MERGE_MISMATCH_MASK) );
			}
		}
	}
	
	protected static void mergeInto( SimpleDirectory dest, Directory src, int flags ) {
		for( Iterator ei=src.getDirectoryEntrySet().iterator(); ei.hasNext(); ) {
			Directory.Entry srcEntry = (Directory.Entry)ei.next();
			SimpleDirectory.Entry existingEntry = (SimpleDirectory.Entry)dest.getDirectoryEntry(srcEntry.getName());
			if( existingEntry == null ) {
				existingEntry = new SimpleDirectory.Entry();
				existingEntry.name = srcEntry.getName();
				dest.addEntry(existingEntry);
			}
			mergeIntoEntry( existingEntry, srcEntry, flags );
		}
	}
	
	public Response call(Map argumentExpressions) {
		List dirs = getPositionalArgumentValues(argumentExpressions);
		SimpleDirectory result = new SimpleDirectory();
		
		int fileFlags = MERGE_FILE_STRICT;
		int dirFlags = MERGE_DIR_MERGE;
		int mismatchFlags = MERGE_MISMATCH_FAIL;
		int strictFlags = 0;
		
		String dirMerge = ValueUtil.getString(getArgumentValue(argumentExpressions, "dir-merge-method", null));
		if( dirMerge == null );
		else if( CcouchNamespace.RR_DIRMERGE_IGNORE.equals(dirMerge) ) dirFlags = MERGE_DIR_IGNORE;
		else if( CcouchNamespace.RR_DIRMERGE_REPLACE.equals(dirMerge) ) dirFlags = MERGE_DIR_REPLACE;
		else if( CcouchNamespace.RR_DIRMERGE_MERGE.equals(dirMerge) ) dirFlags = MERGE_DIR_MERGE;
		else throw new RuntimeException("Unrecognised dir merge method: " + dirMerge);
		
		String fileMerge = ValueUtil.getString(getArgumentValue(argumentExpressions, "file-merge-method", null));
		if( fileMerge == null ) {
		} else if( CcouchNamespace.RR_FILEMERGE_STRICTRE.equals(fileMerge) ) {
			fileFlags = MERGE_FILE_STRICT;
			strictFlags = MERGE_FILE_REPLACEONSAME;
		} else if( CcouchNamespace.RR_FILEMERGE_IGNORE.equals(fileMerge) ) {
			fileFlags = MERGE_FILE_IGNORE;
		} else if( CcouchNamespace.RR_FILEMERGE_REPLACE.equals(fileMerge) ) {
			fileFlags = MERGE_FILE_REPLACE;
		} else {
			throw new RuntimeException("Unrecognised file merge method: " + fileMerge);
		}

		String mismatchMerge = ValueUtil.getString(getArgumentValue(argumentExpressions, "mismatch-merge-method", null));
		if( mismatchMerge == null ) {
		} else if( CcouchNamespace.RR_FILEMERGE_FAIL.equals(mismatchMerge) ) {
			mismatchFlags = MERGE_MISMATCH_FAIL;
		} else if( CcouchNamespace.RR_FILEMERGE_IGNORE.equals(mismatchMerge) ) {
			mismatchFlags = MERGE_MISMATCH_IGNORE;
		} else if( CcouchNamespace.RR_FILEMERGE_REPLACE.equals(mismatchMerge) ) {
			mismatchFlags = MERGE_MISMATCH_REPLACE;
		} else {
			throw new RuntimeException("Unrecognised mismatch merge method: " + mismatchMerge);
		}
		
		int flags = fileFlags | dirFlags | mismatchFlags | strictFlags;

		for( Iterator i=dirs.iterator(); i.hasNext(); ) {
			Directory indir = (Directory)i.next();
			mergeInto( result, indir, flags );
		}
		return new BaseResponse(Response.STATUS_NORMAL, result);
	}
}
