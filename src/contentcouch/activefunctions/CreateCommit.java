package contentcouch.activefunctions;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.date.DateUtil;
import contentcouch.misc.SimpleCommit;
import contentcouch.misc.ValueUtil;

public class CreateCommit extends BaseActiveFunction {

	protected List filter(List l) {
		List r = new ArrayList();
		for( Iterator i=l.iterator(); i.hasNext(); ) {
			Object v = i.next();
			if( v != null ) r.add(v);
		}
		return r;
	}
	
	public Response call(Map argumentExpressions) {
		String message = ValueUtil.getString(getArgumentValue(argumentExpressions, "message", null));
		String author = ValueUtil.getString(getArgumentValue(argumentExpressions, "author", null));
		Date date = DateUtil.getDate(getArgumentValue(argumentExpressions, "date", null));
		Object target = getArgumentValue(argumentExpressions, "target", null);
		List parents = getPositionalArgumentValues(argumentExpressions, "parent");
		if( target == null ) target = getArgumentValue(argumentExpressions, "operand", null);
		if( date == null ) date = new Date(); // Right now!
		
		SimpleCommit commit = new SimpleCommit();
		commit.target = target;
		commit.parents = filter(parents).toArray();
		commit.author = author;
		commit.message = message;
		commit.date = date;
		return new BaseResponse(commit);
	}

}
