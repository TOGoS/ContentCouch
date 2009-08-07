package contentcouch.builtindata;

import java.net.URL;

import contentcouch.misc.ValueUtil;
import contentcouch.store.TheGetter;

import togos.mf.RequestVerbs;
import togos.mf.ResponseCodes;
import togos.mf.Request;
import togos.mf.Response;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;

public class BuiltInData {
	public static Response getResponse(String name) {
		URL resourceUrl = BuiltInData.class.getResource(name);
		if( resourceUrl == null ) {
		    return new BaseResponse(ResponseCodes.RESPONSE_DOESNOTEXIST,
		       "No such resource: " + name, "text/plain");
		}
		Request req = new BaseRequest(RequestVerbs.VERB_GET, resourceUrl.toString());
		return TheGetter.handleRequest(req);
	}
	
	public static String getString(String name) {
		return ValueUtil.getString(TheGetter.getResponseValue(getResponse(name), name));
	}
}
