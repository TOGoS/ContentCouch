package contentcouch.active;

import java.util.HashMap;
import java.util.Stack;

public class Context extends HashMap {
	public static final String GENERIC_GETTER_VAR = "ccouch:getter";
	public static final String IDENTIFIER_VAR = "ccouch:identifier";

	protected HashMap varStacks = new HashMap();;
	
	public Context() {
		super();
	}
	
	public Context( Context cloneFrom ) {
		super( cloneFrom );
	}
	
	public void push( String name, Object value ) {
		Stack stack = (Stack)varStacks.get(name);
		if( stack == null ) {
			stack = new Stack();
			varStacks.put(name, stack);
		}
		stack.push(get(name));
		put(name,value);
	}
	
	public Object pop( String name ) {
		Stack stack = (Stack)varStacks.get(name);
		if( stack == null || stack.size() == 0 ) return null;
		Object oldVal = get(get(name));
		put( name, stack.pop() );
		return oldVal;
	}
	
	public static ThreadLocal instanceVar = new ThreadLocal() {
		protected Object initialValue() {
			return new Context();
		}
	};

	public static Context getInstance() {
		return (Context)instanceVar.get();
	}
	
	/** Creates a thread that will have a Context that is a clone of the current thread's. */  
	public Thread createThread( final Runnable r ) {
		final Context ctx = new Context(getInstance());
		return new Thread( new Runnable() {
			public void run() {
				Context.instanceVar.set(ctx);
				r.run();
			}
		});
	}
}
