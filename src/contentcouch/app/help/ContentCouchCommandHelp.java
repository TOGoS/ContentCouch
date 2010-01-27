package contentcouch.app.help;

import java.net.URL;

import contentcouch.misc.ValueUtil;
import contentcouch.store.TheGetter;

import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;

public class ContentCouchCommandHelp {
	public static Response getResponse(String name) {
		URL resourceUrl = ContentCouchCommandHelp.class.getResource(name+".txt");
		if( resourceUrl == null ) {
		    return new BaseResponse(ResponseCodes.RESPONSE_DOESNOTEXIST,
		       "No such resource: " + name, "text/plain");
		}
		Request req = new BaseRequest(RequestVerbs.VERB_GET, resourceUrl.toString());
		return TheGetter.call(req);
	}
	
	public static String getString(String name) {
		String s = ValueUtil.getString(TheGetter.getResponseValue(getResponse(name), name));
		if( s != null ) s = s.trim();
		return s;
	}
}
