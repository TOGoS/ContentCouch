package contentcouch.active;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;

public interface ActiveFunction {
	public Response call( Request req, Map argumentExpressions );
	/** Should return true if, with the given arguments, this function would always
	 * give the same result (returned or side-effects) for any request.  This will
	 * usually require that the argument expressions that are used in this function's
	 * calculation are also constant. */
	public boolean isConstant( Map argumentExpressions );
}
