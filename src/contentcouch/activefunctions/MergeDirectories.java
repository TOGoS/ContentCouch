package contentcouch.activefunctions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.directory.MergeUtil;
import contentcouch.misc.SimpleDirectory;
import contentcouch.misc.ValueUtil;
import contentcouch.value.Directory;

public class MergeDirectories extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		List dirs = getPositionalArgumentValues(argumentExpressions);
		SimpleDirectory result = new SimpleDirectory();
		
		MergeUtil.RegularConflictResolver conflictResolver = new MergeUtil.RegularConflictResolver();
		String fileMergeMethod = ValueUtil.getString(getArgumentValue(argumentExpressions, "file-merge-method", null));
		if( fileMergeMethod != null ) conflictResolver.fileMergeMethod = fileMergeMethod;
		String dirMergeMethod = ValueUtil.getString(getArgumentValue(argumentExpressions, "dir-merge-method", null));
		if( dirMergeMethod != null ) conflictResolver.dirMergeMethod = dirMergeMethod;
		
		for( Iterator i=dirs.iterator(); i.hasNext(); ) {
			Directory indir = (Directory)i.next();
			MergeUtil.putAll( result, indir, conflictResolver );
		}
		return new BaseResponse(Response.STATUS_NORMAL, result);
	}
}
