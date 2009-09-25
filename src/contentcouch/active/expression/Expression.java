package contentcouch.active.expression;

import togos.mf.api.Request;
import togos.mf.api.Response;

public interface Expression {
	public Response eval(Request req);
	public String toUri();
	public boolean isConstant();
}
