package contentcouch.store;

import togos.rra.BaseRequest;
import togos.rra.BaseRequestHandler;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.blob.BlobUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.rdf.RdfIO;
import contentcouch.value.Blob;

public class ParseRdfRequestHandler extends BaseRequestHandler {
	public Response handleRequest( Request req ) {
		if( !req.getUri().startsWith("x-parse-rdf:") || !"GET".equals(req.getVerb()) ) {
			return BaseResponse.RESPONSE_UNHANDLED;
		}
		
		BaseRequest subReq = new BaseRequest(req, req.getUri().substring("x-parse-rdf:".length()));
		Response subRes = TheGetter.handleRequest(subReq);
		
		if( subRes.getStatus() != Response.STATUS_NORMAL ) return subRes;
		
		Blob blob = BlobUtil.getBlob(subRes.getContent());
		Object value = RdfIO.parseRdf(ValueUtil.getString(blob), subReq.getUri());
		BaseResponse res = new BaseResponse(Response.STATUS_NORMAL, value, subRes);
		res.putMetadata(CcouchNamespace.PARSED_FROM, blob);
		return res;
	}
}
