package contentcouch.activefunctions;

import java.util.HashMap;
import java.util.Map;

import togos.rra.BaseRequest;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.Context;
import contentcouch.explorify.BaseUriProcessor;
import contentcouch.explorify.UriProcessor;
import contentcouch.misc.ValueUtil;
import contentcouch.store.TheGetter;
import contentcouch.active.expression.Expression;
import contentcouch.active.expression.UriExpression;

public class ApplyUriProcessor extends BaseActiveFunction {
	protected String innerProcess( Expression operator, String inputUri ) {
		Context.pushNewDynamicScope();
		try {
			Context.put("uri", inputUri);
			Response res = operator.eval();
			if( res.getStatus() != Response.STATUS_NORMAL ) {
				throw new RuntimeException("Error while processing URI:" + inputUri + ": " + BaseResponse.getErrorSummary(res));
			}
			String newUri = ValueUtil.getString(res.getContent());
			if( newUri == null ) {
				throw new RuntimeException("URI processing expression returned nothing");
			}
			return newUri;
		} finally {
			Context.popInstance();
		}
	}
	
	protected String outerProcess( Map argumentExpressions, String inputUri ) {
		HashMap newArgumentExpressions = new HashMap(argumentExpressions);
		newArgumentExpressions.put("operand", new UriExpression(inputUri));
		return toCallExpression(newArgumentExpressions).toUri();
	}
	
	public Response call( final Map argumentExpressions ) {
		final Expression operator = (Expression)argumentExpressions.get("operator");
		if( operator == null ) throw new RuntimeException( "'operator' expression needed");
		Expression operand = (Expression)argumentExpressions.get("operand");
		if( operator == null ) throw new RuntimeException( "'operand' expression needed");
		final String inputUri = operand.toUri();
		
		String whichOne = null;
		
		UriProcessor oldUriProc = BaseUriProcessor.getInstance(whichOne);
		Context.pushNewDynamicScope();
		try {
			BaseUriProcessor.setInstance(whichOne, new BaseUriProcessor(oldUriProc, true) {
				public String processUri(String uri) {
					return super.processUri(outerProcess( argumentExpressions, innerProcess(operator, uri) ));
				}
			});
			String uri = innerProcess(operator, inputUri);
			BaseRequest req = new BaseRequest(Request.VERB_GET, uri);
			req.contextVars = Context.getInstance();
			return TheGetter.handleRequest(req);
		} finally {
			Context.popInstance();
		}
	}
	
	protected String getPathArgumentName() {
		return "operand";
	}
}
