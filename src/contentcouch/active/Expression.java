package contentcouch.active;

import togos.rra.Response;

public interface Expression {
	public Response eval();
	public String toUri();
}
