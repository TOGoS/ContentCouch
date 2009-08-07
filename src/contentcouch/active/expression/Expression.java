package contentcouch.active.expression;

import togos.mf.Response;

public interface Expression {
	public Response eval();
	public String toUri();
	public boolean isConstant();
}
