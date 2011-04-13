package contentcouch.rdf;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import togos.mf.value.Blob;
import contentcouch.blob.BlobUtil;
import contentcouch.framework.BaseRequestHandler;
import contentcouch.framework.TheGetter;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;

public class ParseRDFRequestHandler extends BaseRequestHandler {
	public Response call( Request req ) {
		if( !"GET".equals(req.getVerb()) ) {
			return BaseResponse.RESPONSE_UNHANDLED;
		}
		
		String parsedUri = UriUtil.stripRdfSubjectPrefix(req.getResourceName());
		if( parsedUri == null ) {
			return BaseResponse.RESPONSE_UNHANDLED;
		}
		
		BaseRequest subReq = new BaseRequest(req, parsedUri);
		Response subRes = TheGetter.call(subReq);
		
		if( subRes.getStatus() != ResponseCodes.NORMAL ) return subRes;
		
		Blob blob = BlobUtil.getBlob(subRes.getContent());
		Object value = RdfIO.parseRdf(ValueUtil.getString(blob), subReq.getResourceName());
		BaseResponse res = new BaseResponse(ResponseCodes.NORMAL, value, subRes);
		res.putContentMetadata(CCouchNamespace.PARSED_FROM, blob);
		return res;
	}
}
