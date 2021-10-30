package contentcouch.app.servlet2.comp;

import java.util.HashMap;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.base.BaseResponse;
import togos.mf.value.Arguments;
import contentcouch.misc.UriUtil;

public class JunkComponent extends BaseComponent {
	public JunkComponent(Map config) {
		properties = new HashMap(config);
		if( properties.get("title") == null ) properties.put("title", "Junk component");
		if( properties.get("path") == null ) properties.put("path", "x-internal:junk/");
		
		this.handlePath = (String)properties.get("path");
	}
	
	public Map getProperties() {
		return properties;
	}
	
	public String getUriFor( Arguments args ) {
		String text = (String)args.getNamedArguments().get("text");
		return this.handlePath + UriUtil.uriEncode(text);
	}
	
	public Response _call(Request req) {
		String rn = req.getResourceName();
		String text = UriUtil.uriDecode(rn.substring(this.handlePath.length()));
		return new BaseResponse(200, "You win a piece of " + text, "text/plain");
	}
}
