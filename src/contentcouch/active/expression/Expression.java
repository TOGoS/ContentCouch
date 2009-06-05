package contentcouch.active.expression;

import togos.rra.Response;

public interface Expression {
	public Response eval();
	public String toUri();
	public boolean isConstant();
}
