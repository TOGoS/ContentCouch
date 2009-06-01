package contentcouch.active;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.misc.UriUtil;

public class ValueExpression implements Expression {
	public Object value;
	public Map metadata = Collections.EMPTY_MAP;
	
	public ValueExpression( Object value ) {
		this.value = value;
	}
	
	public void putMetadata(String key, Object value) {
		if( metadata == Collections.EMPTY_MAP ) metadata = new HashMap();
		metadata.put(key, value);
	}

	public Response eval() {
		BaseResponse bre = new BaseResponse( Response.STATUS_NORMAL, value );
		bre.metadata = metadata;
		return bre;
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
	
	public String toUri() {
		return "data:," + UriUtil.uriEncode(value.toString());
	}
}
