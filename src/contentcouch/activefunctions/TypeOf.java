package contentcouch.activefunctions;

import java.util.Map;

import contentcouch.active.ActiveFunction;
import contentcouch.active.Expression;
import contentcouch.rdf.RdfNamespace;
import contentcouch.value.MetadataHaver;

public class TypeOf implements ActiveFunction {
	public Object call(Map context, Map argumentExpressions) {
		Expression operand = (Expression)argumentExpressions.get("operand");
		if( operand == null ) return null;
		Object obj = operand.eval(context);
		if( obj instanceof MetadataHaver ) {
			return ((MetadataHaver)obj).getMetadata(RdfNamespace.DC_FORMAT);
		}
		if( obj instanceof String ) {
			return "text/plain";
		}
		// TODO: Try to guess type
		return null;
	}
}
