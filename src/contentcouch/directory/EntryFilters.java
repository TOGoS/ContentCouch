package contentcouch.directory;

import contentcouch.misc.Function1;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Blob;
import contentcouch.value.Directory;

public class EntryFilters {
	public static Function1 BLOBFILTER = new Function1() {
		public Object apply(Object input) {
			Directory.Entry e = (Directory.Entry)input;
			if( e == null ) {
				return null;
			} else if( e.getTargetType() == null ) {
				if( e.getTarget() instanceof Blob ) return e;
			} else if( CcouchNamespace.OBJECT_TYPE_BLOB.equals(e.getTargetType()) ) {
				return e;
			}
			return null;
		}
	};
}
