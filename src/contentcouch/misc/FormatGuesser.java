package contentcouch.misc;

import contentcouch.framework.BaseRequestHandler;
import contentcouch.rdf.DcNamespace;
import togos.mf.api.Callable;
import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.base.BaseResponse;

public class FormatGuesser extends BaseRequestHandler
{
	Callable next;
	
	public FormatGuesser(Callable next) {
		this.next = next;
	}
	
	public Response call( Request req ) {
		Response res = next.call(req);
		if( res.getContentMetadata().get(DcNamespace.DC_FORMAT) == null ) {
			String type = MetadataUtil.getContentType(res, req.getResourceName());
			if( type != null ) {
				BaseResponse nr = new BaseResponse(res);
				nr.putContentMetadata(DcNamespace.DC_FORMAT, type);
				res = nr;
			}
		}
		return res;
	}
}
