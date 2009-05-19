package contentcouch.activefunctions;

import java.util.Map;

import contentcouch.active.BaseActiveFunction;
import contentcouch.rdf.RdfNamespace;
import contentcouch.value.MetadataHaver;

public class TypeOf extends BaseActiveFunction {
	public Object call(Map context, Map argumentExpressions) {
		Object obj = getArgumentValue(context, argumentExpressions, "operand", null);
		
		if( obj instanceof MetadataHaver ) {
			String format = (String)((MetadataHaver)obj).getMetadata(RdfNamespace.DC_FORMAT);
			if( format != null ) return format;
		}
		if( obj instanceof String ) {
			return "text/plain";
		}
		// TODO: Try to guess type
		return null;
	}
}
