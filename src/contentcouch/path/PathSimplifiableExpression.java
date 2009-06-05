package contentcouch.path;

import contentcouch.active.expression.Expression;

public interface PathSimplifiableExpression {
	public Expression simplify();
	public Expression appendPath(String path);
}
