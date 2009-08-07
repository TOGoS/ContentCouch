package contentcouch.active;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import sun.misc.BASE64Decoder;
import togos.mf.ResponseCodes;
import togos.mf.Request;
import togos.mf.Response;
import togos.mf.base.BaseResponse;
import contentcouch.framework.BaseRequestHandler;
import contentcouch.misc.UriUtil;
import contentcouch.rdf.DcNamespace;

public class DataUriResolver extends BaseRequestHandler {
	public static final String DATA_URI_PREFIX = "data:";

	public static byte[] parse(String uri, Map metadataDest) {
		if( !uri.startsWith(DATA_URI_PREFIX) ) return null;
		int commaPos = uri.indexOf(',');
		if( commaPos == -1 ) throw new RuntimeException("Invalid data URI has no comma: " + uri);
		String[] junk = uri.substring(DATA_URI_PREFIX.length(),commaPos).split(";");
		boolean base64Encoded = false;
		for( int i=0; i<junk.length; ++i ) {
			String junkPart = junk[i];
			if( "base64".equals(junkPart) ) {
				base64Encoded = true;
			} else {
				metadataDest.put(DcNamespace.DC_FORMAT, junkPart);
			}
		}
		byte[] data;
		if( base64Encoded ) {
			try {
				data = new BASE64Decoder().decodeBuffer(uri.substring(commaPos+1));
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
		} else {
			data = UriUtil.uriDecodeBytes( uri.substring(commaPos+1) );
		}
		return data;
	}
	
	public Response call( Request req ) {
		String uri = req.getUri();
		if( !uri.startsWith(DATA_URI_PREFIX) ) return BaseResponse.RESPONSE_UNHANDLED;
		HashMap metadata = new HashMap();
		byte[] data = parse(uri, metadata);
		BaseResponse res = new BaseResponse(ResponseCodes.RESPONSE_NORMAL, data);
		res.metadata = metadata;
		return res;
	}
}
