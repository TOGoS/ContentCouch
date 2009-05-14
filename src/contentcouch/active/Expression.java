package contentcouch.active;

import java.util.Map;

public interface Expression {
	public Object eval( Map context );
}
