package contentcouch.active;

public class Bareword implements Expression {
	
	public String text;
	
	public Bareword(String text) {
		this.text = text;
	}
	
	public String toString() {
		return text;
	}
	
	public String toUri() {
		throw new RuntimeException("Bareword cannot be converted to URIs!");
	}

	public Object eval() {
		throw new RuntimeException("Bareword cannot be evaluated!");
	}
}
