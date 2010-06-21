package contentcouch.directory;

import togos.mf.value.Blob;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class CloneUtil {
	public static final int CLONE_TARGETTYPE_UNKNOWN = 0x00;
	public static final int CLONE_TARGETTYPE_BLOB    = 0x01;
	public static final int CLONE_TARGETTYPE_DIR     = 0x02;
	public static final int CLONE_TARGETTYPE_MASK    = 0x03;

	public static final int CLONE_REFTYPE_REF       = 0x00;
	public static final int CLONE_REFTYPE_OBJ       = 0x04;
	public static final int CLONE_REFTYPE_MASK      = 0x04;

	public static final int CLONE_ACTION_IGNORE     = 0x00;
	public static final int CLONE_ACTION_FAIL       = 0x01;
	public static final int CLONE_ACTION_COPY       = 0x02;
	public static final int CLONE_ACTION_CLONE      = 0x03;

	public static final int CLONE_ACTION_MASK       = 0x03;
	public static final int CLONE_ACTION_BITS       = 2;
	
	public static final int CLONE_FLAGS_COPY = mkCloneFlags(CLONE_ACTION_COPY, CLONE_ACTION_COPY, CLONE_ACTION_COPY, CLONE_ACTION_COPY, CLONE_ACTION_COPY, CLONE_ACTION_COPY);

	public static final int mkCloneFlags( int unknownRef, int blobRef, int dirRef, int unknownObj, int blobObj, int dirObj ) {
		int flags = 0;
		flags |= (unknownRef << (CLONE_TARGETTYPE_UNKNOWN | CLONE_REFTYPE_REF));
		flags |= (blobRef    << (CLONE_TARGETTYPE_BLOB    | CLONE_REFTYPE_REF));
		flags |= (dirRef     << (CLONE_TARGETTYPE_DIR     | CLONE_REFTYPE_REF));
		flags |= (unknownObj << (CLONE_TARGETTYPE_UNKNOWN | CLONE_REFTYPE_OBJ));
		flags |= (blobObj    << (CLONE_TARGETTYPE_BLOB    | CLONE_REFTYPE_OBJ));
		flags |= (dirObj     << (CLONE_TARGETTYPE_DIR     | CLONE_REFTYPE_OBJ));
		return flags;
	}
	
	public static final String getTypeDesc( int typeIndex ) {
		String refType;
		
		switch( typeIndex & CLONE_REFTYPE_MASK ) {
		case( CLONE_REFTYPE_OBJ ): refType = "Object"; break;
		default: refType = "Ref";
		}
		
		return getTargetTypeName(typeIndex) + "-" + refType;
	}
	
	public static final String getTargetTypeName( int typeIndex ) {
		switch( typeIndex & CLONE_TARGETTYPE_MASK ) {
		case( CLONE_TARGETTYPE_BLOB ): return CCouchNamespace.TT_SHORTHAND_BLOB;
		case( CLONE_TARGETTYPE_DIR ): return CCouchNamespace.TT_SHORTHAND_DIRECTORY;
		default: return "Unknown";
		}
	}
	
	public static final int getTargetTypeIndex( Directory.Entry e ) {
		return getTargetTypeIndex( e.getTarget(), e.getTargetType() );
	}
	
	public static final int getTargetTypeIndex( Object obj, String type ) {
		if( CCouchNamespace.TT_SHORTHAND_BLOB.equals(type) ) {
			return CLONE_TARGETTYPE_BLOB;
		} else if( CCouchNamespace.TT_SHORTHAND_DIRECTORY.equals(type) ) {
			return CLONE_TARGETTYPE_DIR;
		} else {
			if( obj instanceof Directory ) {
				return CLONE_TARGETTYPE_DIR;
			} else if( obj instanceof Blob || obj instanceof String ) {
				return CLONE_TARGETTYPE_BLOB;
			}
		}
		return CLONE_TARGETTYPE_UNKNOWN;
	}
	
	public static final int getTypeIndex( Object obj, String type ) {
		int idx = getTargetTypeIndex(obj, type);
		if( obj instanceof Ref ) {
			idx |= CLONE_REFTYPE_REF;
		} else {
			idx |= CLONE_REFTYPE_OBJ;
		}
		return idx;
	}
}
