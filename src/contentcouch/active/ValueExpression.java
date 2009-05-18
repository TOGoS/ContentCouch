package contentcouch.active;

import java.util.Map;

public class ValueExpression implements Expression {
	public Object value;
	
	public ValueExpression( Object value ) {
		this.value = value;
	}
	
	public Object eval(Map context) {
		return value;
	}

	public String toString() {
		if( value instanceof Expression ) {
			return "(quote " + value.toString() + ")";
		} else if( value instanceof String ) {
			return "\"" + ((String)value).
				replaceAll("\\\\", "\\\\\\\\").
				replaceAll("\"", "\\\"").
				replaceAll("\n","\\\\n").
				replaceAll("\r","\\\\r").
				replaceAll("\t","\\\\t") + "\"";
		} else if( value instanceof Number ) {
			return value.toString();
		} else {
			throw new RuntimeException("Don't know how to toString thie value: " + value.toString());
		}
	}
}
