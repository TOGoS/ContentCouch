package contentcouch.path;

import java.util.Map;

import contentcouch.active.expression.Expression;

public interface PathSimplifiableActiveFunction {
	public Expression simplify(Map argumentExpressions);
	/**
	 * @param String path path within resource we are trying to reference.
	 *   path should NOT be URI encoded (path components are free to
	 *   contain URI-encoded information, but it will not be treated
	 *   specially at this level).
	 */
	public Expression appendPath(Expression funcExpression, Map argumentExpressions, String path);
}
