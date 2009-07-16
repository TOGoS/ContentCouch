package contentcouch.activefunctions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.Expression;
import contentcouch.directory.DirectoryMerger;
import contentcouch.misc.SimpleDirectory;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;

public class MergeDirectories extends BaseActiveFunction {
	public Response call(Map argumentExpressions) {
		List expressions = getPositionalArgumentExpressions(argumentExpressions);
		SimpleDirectory result = new SimpleDirectory();
		
		DirectoryMerger.RegularConflictResolver conflictResolver = new DirectoryMerger.RegularConflictResolver();
		String fileMergeMethod = ValueUtil.getString(getArgumentValue(argumentExpressions, "file-merge-method", null));
		if( fileMergeMethod != null ) conflictResolver.fileMergeMethod = fileMergeMethod;
		//String dirMergeMethod = ValueUtil.getString(getArgumentValue(argumentExpressions, "dir-merge-method", null));
		//if( dirMergeMethod != null ) conflictResolver.dirMergeMethod = dirMergeMethod;
		conflictResolver.dirMergeMethod = CcouchNamespace.REQ_DIRMERGE_MERGE;
		
		for( Iterator i=expressions.iterator(); i.hasNext(); ) {
			Expression exp = (Expression)i.next();
			String uri = exp.toString();
			Directory indir = (Directory)TheGetter.getResponseValue(exp.eval(), uri);
			new DirectoryMerger( conflictResolver, true ).putAll( result, indir, uri, "x-undefined:new-directory" );
		}
		return new BaseResponse(Response.STATUS_NORMAL, result);
	}
}
