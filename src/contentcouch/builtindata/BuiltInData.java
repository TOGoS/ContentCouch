package contentcouch.builtindata;

import java.net.URL;

import contentcouch.framework.TheGetter;
import contentcouch.misc.ValueUtil;

import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
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
		return TheGetter.call(req);
	}
	
	public static String getString(String name) {
		return ValueUtil.getString(TheGetter.getResponseValue(getResponse(name), name));
	}
}
