package contentcouch.app;

import java.util.Iterator;

public interface ArgumentHandler {
	public boolean handleArguments( String current, Iterator rest );
}
