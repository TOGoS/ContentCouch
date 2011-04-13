package contentcouch.repository;

import contentcouch.framework.TheGetter;
import contentcouch.misc.ValueUtil;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.value.Blob;

public class TheIdentifier {
	public static String identify( Blob b ) {
		BaseRequest req = new BaseRequest(RequestVerbs.POST, "x-ccouch-repo:identify");
		req.content = b;
		Response res = TheGetter.call(req);
		if( res.getStatus() != ResponseCodes.NORMAL ) {
			throw new RuntimeException( "Could not identify blob; " + ValueUtil.getString(res.getContent()));
		}
		return ValueUtil.getString(res.getContent());
	}
}
