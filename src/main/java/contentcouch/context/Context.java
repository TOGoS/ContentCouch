package contentcouch.context;

import java.util.Map;

public class Context {
	protected static ThreadLocal threadLocalInstance = new ThreadLocal();
	
	/**
	 * Ideally we get context vars from the most relevant Request object,
	 * but sometimes when deep inside a function or something we don't have
	 * access to that, so can use this instead:
	 */
	public static Map getInstance() {
		Map inst = (Map)threadLocalInstance.get();
		if( inst == null ) throw new RuntimeException("No context configured on this thread!");
		return inst;
	}
	
	public static void setThreadLocalInstance( Map instance ) {
		threadLocalInstance.set( instance );
	}
}
