package contentcouch.store;

public class PrefixGetFilter implements Getter {
	Getter parent;
	String prefix;
	
	public PrefixGetFilter(Getter parent, String prefix) {
		this.parent = parent;
		this.prefix = prefix;
	}
	
	public Object get(String identifier) {
		return parent.get(prefix + identifier);
	}
}
