package contentcouch.active.expression;

import togos.mf.api.Request;
import togos.mf.api.Response;

public interface Expression {
	public Response eval(Request req);
	public String toUri();
	/** Should return true if eval(req) will always return the same thing, regardless of the request */
	public boolean isConstant();
}
