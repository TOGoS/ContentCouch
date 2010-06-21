package contentcouch.directory;

import togos.mf.value.Blob;
import contentcouch.misc.Function1;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.value.Directory;

public class EntryFilters {
	public static Function1 createTypeFilter( final String typeString, final boolean resolve, final Class klass ) {
		return new Function1() {
			public Object apply(Object input) {
				Directory.Entry e = (Directory.Entry)input;
				if( e == null ) {
					return null;
				} else if( e.getTargetType() == null ) {
					Object target = resolve ? DirectoryUtil.resolveTarget(e) : e.getTarget();
					if( klass.isInstance(target) ) return e;
				} else if( typeString.equals(e.getTargetType()) ) {
					return e;
				}
				return null;
			}
		};
	}
	
	public static Function1 BLOBFILTER = createTypeFilter( CCouchNamespace.TT_SHORTHAND_BLOB, false, Blob.class );
	
	public static Function1 DIRECTORYFILTER = createTypeFilter( CCouchNamespace.TT_SHORTHAND_DIRECTORY, false, Directory.class );
}
