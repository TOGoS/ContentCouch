package contentcouch.active;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;

public interface ActiveFunction {
	public Response call( Request req, Map argumentExpressions );
}
