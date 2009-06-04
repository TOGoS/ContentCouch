package contentcouch.active;

import java.io.IOException;

import sun.misc.BASE64Decoder;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.RequestHandler;
import togos.rra.Response;
import contentcouch.blob.ByteArrayBlob;
import contentcouch.misc.UriUtil;

public class DataUriResolver implements RequestHandler {
	public static final String DATA_URI_PREFIX = "data:";

	public Response handleRequest( Request req ) {
		String identifier = req.getUri();
		if( !identifier.startsWith(DATA_URI_PREFIX) ) return BaseResponse.RESPONSE_UNHANDLED;
		int commaPos = identifier.indexOf(',');
		if( commaPos == -1 ) throw new RuntimeException("Invalid data URI has no comma: " + identifier);
		String[] junk = identifier.substring(DATA_URI_PREFIX.length(),commaPos).split(";");
		boolean base64Encoded = false;
		String type = null;
		for( int i=0; i<junk.length; ++i ) {
			String junkPart = junk[i];
			if( "base64".equals(junkPart) ) {
				base64Encoded = true;
			} else {
				type = junkPart;
			}
		}
		byte[] data;
		if( base64Encoded ) {
			try {
				data = new BASE64Decoder().decodeBuffer(identifier.substring(commaPos+1));
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
		} else {
			data = UriUtil.uriDecodeBytes( identifier.substring(commaPos+1) );
		}
		return new BaseResponse(Response.STATUS_NORMAL, new ByteArrayBlob(data), type );
	}
}
