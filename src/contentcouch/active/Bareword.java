package contentcouch.active;

import java.util.Map;

public class Bareword implements Expression {
	
	public String text;
	
	public Bareword(String text) {
		this.text = text;
	}
	
	public String toString() {
		return text;
	}

	public Object eval(Map context) {
		throw new RuntimeException("Bareword cannot be evaluated!");
	}
}
