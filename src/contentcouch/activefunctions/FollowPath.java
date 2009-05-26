package contentcouch.activefunctions;

import java.util.Map;

import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.ValueUtil;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class FollowPath extends BaseActiveFunction {
	public Object call(Map argumentExpressions) {
		Object source = getArgumentValue(argumentExpressions, "source", null);
		if( source == null ) throw new RuntimeException("No source");
		String path = ValueUtil.getString(getArgumentValue(argumentExpressions, "path", null));
		if( path == null ) throw new RuntimeException("No path");
		
		String[] pathParts = path.split("/+");
		for( int i=0; i<pathParts.length; ++i ) {
			if( source instanceof Ref ) {
				source = TheGetter.get( ((Ref)source).targetUri );
			}
			if( source instanceof Directory ) {
				Directory.Entry e = ((Directory)source).getEntry(pathParts[i]);
				if( e == null ) return "Not found for " + pathParts[i];
				source = e.getValue();
			} else {
				return null;
			}
		}
		return source;
	}
}
