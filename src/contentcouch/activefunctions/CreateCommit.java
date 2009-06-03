package contentcouch.activefunctions;

import java.util.Date;
import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.date.DateUtil;
import contentcouch.misc.SimpleCommit;
import contentcouch.misc.ValueUtil;

public class CreateCommit extends BaseActiveFunction {

	public Response call(Map argumentExpressions) {
		String message = ValueUtil.getString(getArgumentValue(argumentExpressions, "message", null));
		String author = ValueUtil.getString(getArgumentValue(argumentExpressions, "author", null));
		Date date = DateUtil.getDate(getArgumentValue(argumentExpressions, "author", null));
		Object target = getArgumentValue(argumentExpressions, "target", null);
		if( target == null ) target = getArgumentValue(argumentExpressions, "operand", null);
		if( date == null ) date = new Date(); // Right now!
		
		SimpleCommit commit = new SimpleCommit();
		if(author != null) commit.author = author;
		if(message != null) commit.message = message;
		commit.date = date;
		return new BaseResponse(commit);
	}

}
