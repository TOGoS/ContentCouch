package contentcouch.activefunctions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.Expression;
import contentcouch.directory.DirectoryMerger;
import contentcouch.directory.SimpleDirectory;
import contentcouch.framework.TheGetter;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.value.Directory;

public class MergeDirectories extends BaseActiveFunction {
	public Response call(Request req, Map argumentExpressions) {
		List expressions = getPositionalArgumentExpressions(argumentExpressions);
		SimpleDirectory result = new SimpleDirectory();
		
		DirectoryMerger.RegularConflictResolver conflictResolver = new DirectoryMerger.RegularConflictResolver();
		String fileMergeMethod = ValueUtil.getString(getArgumentValue(req, argumentExpressions, "file-merge-method", null));
		if( fileMergeMethod != null ) conflictResolver.fileMergeMethod = fileMergeMethod;
		//String dirMergeMethod = ValueUtil.getString(getArgumentValue(argumentExpressions, "dir-merge-method", null));
		//if( dirMergeMethod != null ) conflictResolver.dirMergeMethod = dirMergeMethod;
		conflictResolver.dirMergeMethod = CCouchNamespace.REQ_DIRMERGE_MERGE;
		
		for( Iterator i=expressions.iterator(); i.hasNext(); ) {
			Expression exp = (Expression)i.next();
			String uri = exp.toString();
			Directory indir = (Directory)TheGetter.getResponseValue(exp.eval(req), uri);
			new DirectoryMerger( conflictResolver, req.getMetadata() ).putAll( result, indir, uri, "x-undefined:new-directory" );
		}
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, result);
	}
}
