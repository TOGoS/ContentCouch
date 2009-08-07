package contentcouch.repository;

import contentcouch.misc.ValueUtil;
import contentcouch.store.TheGetter;
import contentcouch.value.Blob;
import togos.mf.RequestVerbs;
import togos.mf.ResponseCodes;
import togos.mf.Response;
import togos.mf.base.BaseRequest;

public class TheIdentifier {
	public static String identify( Blob b ) {
		BaseRequest req = new BaseRequest(RequestVerbs.VERB_POST, "x-ccouch-repo:identify");
		req.content = b;
		Response res = TheGetter.handleRequest(req);
		if( res.getStatus() != ResponseCodes.RESPONSE_NORMAL ) {
			throw new RuntimeException( "Could not identify blob; " + ValueUtil.getString(res.getContent()));
		}
		return ValueUtil.getString(res.getContent());
	}
}
