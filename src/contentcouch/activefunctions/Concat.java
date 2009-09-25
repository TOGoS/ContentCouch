package contentcouch.activefunctions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import togos.mf.value.Blob;
import contentcouch.active.BaseActiveFunction;
import contentcouch.blob.BlobUtil;
import contentcouch.blob.ByteArrayBlob;

public class Concat extends BaseActiveFunction {

	static long maxLength = 1024*1024; // 1 GB should be enough until we change this to lazy-load data...
	
	public Response call( Request req, Map argumentExpressions ) {
		boolean prev = false;
		long totalLength = 0;
		
		Blob separator = BlobUtil.getBlob(getArgumentValue( req, argumentExpressions, "separator", "" ));
		
		List values = getPositionalArgumentValues(req, argumentExpressions);
		List blobs = new ArrayList();
		for( Iterator i=values.iterator(); i.hasNext(); ) {
			Object v = i.next();
			if( v != null ) {
				if( prev ) {
					blobs.add(separator);
					long addLength = separator.getLength();
					totalLength += addLength;
					if( addLength > maxLength || totalLength > maxLength ) {
						throw new RuntimeException("Concatted blob becomes too long!");
					}
				}
				Blob b = BlobUtil.getBlob(v);
				blobs.add( b );
				totalLength += b.getLength();
				prev = true;
			}
		}
		
		byte[] resultBytes = new byte[(int)totalLength];
		int offset=0;
		for( Iterator i=blobs.iterator(); i.hasNext(); ) {
			Blob blob = (Blob)i.next();
			int len = (int)blob.getLength();
			byte[] blobBytes = blob.getData(0, len);
			for( int j=0; j<len; ++j, ++offset ) resultBytes[offset] = blobBytes[j];
		}

		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, new ByteArrayBlob(resultBytes));
	}
}
