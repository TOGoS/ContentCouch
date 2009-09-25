package contentcouch.activefunctions;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
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
	
	public Response call(Request req, Map argumentExpressions) {
		String message = ValueUtil.getString(getArgumentValue(req, argumentExpressions, "message", null));
		String author = ValueUtil.getString(getArgumentValue(req, argumentExpressions, "author", null));
		Date date = DateUtil.getDate(getArgumentValue(req, argumentExpressions, "date", null));
		Object target = getArgumentValue(req, argumentExpressions, "target", null);
		List parents = getPositionalArgumentValues(req, argumentExpressions, "parent");
		if( target == null ) target = getArgumentValue(req, argumentExpressions, "operand", null);
		if( date == null ) date = new Date(); // Right now!
		
		SimpleCommit commit = new SimpleCommit();
		commit.target = target;
		commit.parents = filter(parents).toArray();
		commit.author = author;
		commit.message = message;
		commit.date = date;
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, commit);
	}

}
