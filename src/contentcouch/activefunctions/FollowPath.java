package contentcouch.activefunctions;

import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;

import contentcouch.active.BaseActiveFunction;
import contentcouch.misc.ValueUtil;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class FollowPath extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
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
				Directory.Entry e = ((Directory)source).getDirectoryEntry(pathParts[i]);
				if( e == null ) return new BaseResponse(pathParts[i] + " not found");
				source = e.getValue();
			} else {
				return new BaseResponse(Response.STATUS_DOESNOTEXIST, "Cannot follow path " + path);
			}
		}
		return new BaseResponse(source);
	}
}
