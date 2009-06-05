package contentcouch.path;

import java.util.Map;

import contentcouch.active.expression.Expression;

public interface PathSimplifiableActiveFunction {
	public Expression simplify(Map argumentExpressions);
	public Expression appendPath(Map argumentExpressions, String path);
}
