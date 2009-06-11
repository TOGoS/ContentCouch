package contentcouch.active;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Context {
	//// Instance stuff ////
	
	protected static ThreadLocal instanceStack = new ThreadLocal() {
		protected Object initialValue() {
			Stack s = new Stack();
			s.push(new HashMap());
			return s;
		}		
	};
	
	public static void pushInstance(Map ctx) {
		((Stack)instanceStack.get()).push(ctx);
	}
	public static void pushInstance() {
		pushInstance(new HashMap());
	}
	public static void popInstance() {
		((Stack)instanceStack.get()).pop();
	}
	public static Map getInstance() {
		return (Map)((Stack)instanceStack.get()).peek();
	}	
	public static void pushNewDynamicScope() {
		pushInstance(new HashMap(getInstance()));
	}
	
	//// Var stuff ////
	
	public static Object get(String key) {
		return getInstance().get(key);
	}
	public static Object put(String key, Object value) {
		return getInstance().put(key, value);
	}
	
	protected static ThreadLocal varStacks = new ThreadLocal() {
		protected Object initialValue() {
			return new HashMap();
		}		
	};
	
	protected static Stack getVarStack( String key ) {
		Stack s = (Stack)((HashMap)varStacks.get()).get(key);
		if( s == null ) {
			s = new Stack();
			((HashMap)varStacks.get()).put(key, s);
		}
		return s;
	}
	
	public static void push(String key, Object value) {
		getVarStack(key).push(get(key));
		put(key, value);
	}

	public static Object pop(String key) {
		Object rv = get(key);
		put( key, getVarStack(key).pop() );
		return rv;
	}
}
