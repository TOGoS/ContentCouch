package contentcouch.active;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Context {
	public static final String GENERIC_GETTER_VAR = "ccouch:getter";
	public static final String IDENTIFIER_VAR = "ccouch:identifier";

	protected static Stack instanceStack = new Stack();
	public static void pushInstance(Map ctx) {
		instanceVar.set(ctx);
	}
	public static void pushInstance() {
		pushInstance(new HashMap());
	}
	public static void popInstance() {
		instanceVar.set(instanceStack.pop());
	}
	
	protected HashMap varStacks = new HashMap();
	
	public static ThreadLocal instanceVar = new ThreadLocal() {
		protected Object initialValue() {
			return new HashMap();
		}
	};

	public static Map getInstance() {
		return (Map)instanceVar.get();
	}
	
	public static Object get(String key) {
		return getInstance().get(key);
	}
}
