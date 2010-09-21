package contentcouch.path;

import contentcouch.active.expression.Expression;

public interface PathSimplifiableExpression {
	public Expression simplify();
	/**
	 * @param String path path within resource we are trying to reference.
	 *   path should NOT be URI encoded (path components are free to
	 *   contain URI-encoded information, but it will not be treated
	 *   specially at this level).
	 */
	public Expression appendPath(String path);
}
