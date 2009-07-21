package contentcouch.active;

import java.util.Map;

import togos.rra.Response;

public interface ActiveFunction {
	public Response call( Map argumentExpressions );
}
