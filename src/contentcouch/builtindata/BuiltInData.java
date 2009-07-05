package contentcouch.builtindata;

import java.net.URL;

import contentcouch.misc.ValueUtil;
import contentcouch.store.TheGetter;

import togos.rra.BaseRequest;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;

public class BuiltInData {
	public static Response getResponse(String name) {
		URL resourceUrl = BuiltInData.class.getResource(name);
		if( resourceUrl == null ) {
		    return new BaseResponse(Response.STATUS_DOESNOTEXIST,
		       "No such resource: " + name, "text/plain");
		}
		Request req = new BaseRequest(Request.VERB_GET, resourceUrl.toString());
		return TheGetter.handleRequest(req);
	}
	
	public static String getString(String name) {
		return ValueUtil.getString(TheGetter.getResponseValue(getResponse(name), name));
	}
}
