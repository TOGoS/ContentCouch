package contentcouch.directory;

import java.util.Comparator;

import com.eekboom.utils.Strings;

import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Directory;
import contentcouch.value.Directory.Entry;

public class EntryComparators {
	public static Comparator NAME_COMPARATOR = new Comparator() {
		public int compare(Object o1, Object o2) {
			Directory.Entry e1 = (Directory.Entry)o1;
			Directory.Entry e2 = (Directory.Entry)o2;
			return Strings.compareNatural(e1.getName(), e2.getName());
		}
	};
	
	public static Comparator TYPE_THEN_NAME_COMPARATOR = new Comparator() {
		protected int compareTargetTypes( String tt1, String tt2 ) {
			if( tt1 == null && tt2 == null ) return 0;
			if( tt1 == null ) return 1;
			if( tt2 == null ) return -1;
			if( tt1.equals(tt2) ) return 0;
			if( tt1.equals(CcouchNamespace.OBJECT_TYPE_DIRECTORY) ) return -1;
			if( tt2.equals(CcouchNamespace.OBJECT_TYPE_DIRECTORY) ) return 1;
			return 0;
		}
		
		public int compare(Object o1, Object o2) {
			Entry e1 = (Entry)o1;
			Entry e2 = (Entry)o2;
			
			int ttc = compareTargetTypes(e1.getTargetType(), e2.getTargetType());
			if( ttc == 0 ) return Strings.compareNatural(e1.getName(),e2.getName());
			return ttc;
		}
	};
}
