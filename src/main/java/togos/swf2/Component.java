package togos.swf2;

import java.util.Map;

import togos.mf.api.Callable;
import togos.mf.value.Arguments;

public interface Component extends Callable {
	/** Return assorted metadata about this component that may be useful to other components. */
	public Map getProperties();	

	/** Return the URI for a page generated by this component based on the given
	 * component-specific arguments.
	 * 
	 * The returned URI is an internal URI and will usually need to be processed
	 * before being returned.
	 * */
	public String getUriFor(Arguments args);
}