package contentcouch.repository;

import contentcouch.misc.ValueUtil;
import contentcouch.store.TheGetter;
import contentcouch.value.Blob;
import togos.rra.BaseRequest;
import togos.rra.Request;
import togos.rra.Response;

public class TheIdentifier {
	public static String identify( Blob b ) {
		BaseRequest req = new BaseRequest(Request.VERB_POST, "x-ccouch-repo:identify");
		req.content = b;
		Response res = TheGetter.handleRequest(req);
		if( res.getStatus() != Response.STATUS_NORMAL ) {
			throw new RuntimeException( "Could not identify blob; " + ValueUtil.getString(res.getContent()));
		}
		return ValueUtil.getString(res.getContent());
	}
}
