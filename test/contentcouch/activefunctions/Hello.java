package contentcouch.activefunctions;

import java.util.Map;

import contentcouch.active.ActiveFunction;

public class Hello implements ActiveFunction {
	public Object call( Map argumentExpressions ) {
		return "Hello, world";
	}
}
