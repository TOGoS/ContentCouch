package contentcouch.active;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;


public abstract class BaseActiveFunction implements ActiveFunction {
	protected Response getArgumentResponse( Map argumentExpressions, String name ) {
		Expression e = (Expression)argumentExpressions.get(name);
		if( e == null ) return new BaseResponse(Response.STATUS_DOESNOTEXIST, "Missing argument " + name, "text/plain");
		return e.eval();
	}
	
	protected Object getArgumentValue( Map argumentExpressions, String name, Object defaultValue ) {
		Expression e = (Expression)argumentExpressions.get(name);
		if( e == null ) return defaultValue;
		Response res = e.eval();
		if( res.getStatus() == Response.STATUS_NORMAL ) return e.eval().getContent();
		return defaultValue;
	}
	
	protected List getPositionalArgumentExpressions( Map argumentExpressions ) {
		List l = new ArrayList();
		for( Iterator i=argumentExpressions.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			String s = (String)e.getKey();
			if( s.startsWith("operand") ) {
				String numericPart = s.substring(7);
				int pos;
				if( numericPart.length() == 0 ) {
					pos = 0;
				} else {
					pos = Integer.parseInt(numericPart);
				}
				if( pos > 128 ) throw new RuntimeException("Too many positional arguments");
				while( l.size() < pos ) l.add(null);
				l.add(pos, e.getValue());
			}
		}
		return l;
	}

	protected List getPositionalArgumentValues( Map argumentExpressions ) {
		List l = new ArrayList();
		for( Iterator i=argumentExpressions.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			String s = (String)e.getKey();
			if( s.startsWith("operand") ) {
				String numericPart = s.substring(7);
				int pos;
				if( numericPart.length() == 0 ) {
					pos = 0;
				} else {
					pos = Integer.parseInt(numericPart);
				}
				if( pos > 128 ) throw new RuntimeException("Too many positional arguments");
				Expression exp = (Expression)e.getValue();
				if( exp != null ) {
					while( l.size() < pos ) l.add(null);
					l.add(pos, exp.eval());
				}
			}
		}
		return l;
	}
}
