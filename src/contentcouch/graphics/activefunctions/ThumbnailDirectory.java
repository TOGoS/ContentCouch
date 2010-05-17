package contentcouch.graphics.activefunctions;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.Expression;
import contentcouch.value.Ref;

public class ThumbnailDirectory extends BaseActiveFunction {
	public Response call( Request req, Map argumentExpressions ) {
		Expression o = (Expression)argumentExpressions.get("operand");
		contentcouch.graphics.ThumbnailDirectory td;
		if( o instanceof Ref ) {
			td = new contentcouch.graphics.ThumbnailDirectory( (Ref)o ); 
		} else {
			td = new contentcouch.graphics.ThumbnailDirectory( o.eval(req), o.toUri() );
		}
		return new BaseResponse( ResponseCodes.RESPONSE_NORMAL, td );
	}
}
