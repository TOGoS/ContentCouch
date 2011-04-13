package contentcouch.active.expression;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;

public class ValueExpression implements Expression {
	public Object value;
	public Map metadata = Collections.EMPTY_MAP;
	
	public ValueExpression( Object value ) {
		this.value = value;
	}
	
	public ValueExpression( Object value, Map metadata ) {
		this.value = value;
		this.metadata = metadata;
	}

	public void putMetadata(String key, Object value) {
		if( metadata == Collections.EMPTY_MAP ) metadata = new HashMap();
		metadata.put(key, value);
	}

	public Response eval( Request req ) {
		BaseResponse bre = new BaseResponse( ResponseCodes.NORMAL, value );
		bre.metadata = metadata;
		return bre;
	}

	public String toString() {
		if( value instanceof byte[] ) {
			value = ValueUtil.getString((byte[])value);
		}
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
		return "data:," + UriUtil.uriEncode(ValueUtil.getBytes(value));
	}
	
	public boolean isConstant() {
		return true;
	}
}
