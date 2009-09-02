package contentcouch.app.servlet2.comp;

import java.util.HashMap;
import java.util.Map;

import contentcouch.misc.UriUtil;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.base.BaseResponse;
import togos.mf.value.Arguments;
import togos.swf2.Component;

public class JunkComponent implements Component {
	Map properties;
	String handlePath;
	
	public JunkComponent(Map config) {
		this.properties = new HashMap();
		this.properties.put("title", "Junk");
		this.properties.put("path", "x-internal:junk/");
		this.properties.putAll(config);
		
		this.handlePath = (String)properties.get("path");
	}
	
	public Map getProperties() {
		return properties;
	}
	
	@Override
	public String getUriFor(Arguments args) {
		String text = (String)args.getNamedArguments().get("text");
		return this.handlePath + UriUtil.uriEncode(text);
	}
	
	public Response call(Request req) {
		String rn = req.getResourceName();
		if( !rn.startsWith(this.handlePath) ) return BaseResponse.RESPONSE_UNHANDLED;
		String text = UriUtil.uriDecode(rn.substring(this.handlePath.length()));
		return new BaseResponse(200, "You win a piece of " + text, "text/plain");
	}
}
