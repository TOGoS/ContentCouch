package togos.mf.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import togos.mf.Arguments;

public class BaseArguments implements Arguments {
	public static Object single( Object value ) {
		BaseArguments args = new BaseArguments();
		args.addPositionalArgument(value);
		return args;
	}
	
	protected List positionalArguments = Collections.EMPTY_LIST;
	protected Map namedArguments = Collections.EMPTY_MAP;
	
	public BaseArguments() { }
	
	public BaseArguments( List positionalArguments, Map namedArguments ) {
		if( positionalArguments != null ) this.positionalArguments = positionalArguments;
		if( namedArguments != null ) this.namedArguments = namedArguments;
	}

	public void addPositionalArgument( Object value ) {
		if( positionalArguments == Collections.EMPTY_LIST ) positionalArguments = new ArrayList();
		positionalArguments.add(value);
	}
	
	public Map getNamedArguments() {
		return namedArguments;
	}

	public List getPositionalArguments() {
		return positionalArguments;
	}

}
