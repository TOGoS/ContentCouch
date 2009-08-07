package contentcouch.active.expression;

import togos.mf.api.Response;

public interface Expression {
	public Response eval();
	public String toUri();
	public boolean isConstant();
}
