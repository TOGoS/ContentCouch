package contentcouch.http;

import togos.rra.BaseRequestHandler;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.RequestHandler;
import togos.rra.Response;
import togos.rra.RraNamespace;
import contentcouch.directory.DirectoryUtil;
import contentcouch.misc.MapUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Blob;
import contentcouch.value.Directory;

public class HtmlDirectoryResponseFilter extends BaseRequestHandler {
	protected RequestHandler parent;
	
	public HtmlDirectoryResponseFilter(RequestHandler parent) {
		this.parent = parent;
	}
	
	public Response handleRequest(Request req) {
		Response pres = parent.handleRequest(req);
		if( pres.getStatus() != Response.STATUS_NORMAL ) return pres;
		
		if( pres.getContent() instanceof Blob && req.getUri().endsWith("/") && ValueUtil.getBoolean(req.getMetadata().get(CcouchNamespace.RR_HTTP_DIRECTORIES_DESIRED), false)) {
			Directory dir = DirectoryUtil.parseHtmlDirectory( (Blob)pres.getContent(), req.getUri() );
			
			BaseResponse res = new BaseResponse(Response.STATUS_NORMAL, dir);
			if( ValueUtil.getBoolean(MapUtil.getKeyed(pres.getMetadata(), RraNamespace.CACHEABLE), false) ) {
				res.putMetadata(RraNamespace.CACHEABLE, Boolean.TRUE);
			}
			res.putContentMetadata(CcouchNamespace.PARSED_FROM, pres.getContent());
			return res;
		}
		
		return pres;
	}
}
