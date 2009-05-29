package contentcouch.file;

import java.io.File;

import togos.rra.BaseRequestHandler;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.blob.BlobUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Blob;

public class FileRequestHandler extends BaseRequestHandler {

	public Response handleRequest( Request req ) {
		if( !req.getUri().startsWith("file:") ) {
			return BaseResponse.RESPONSE_UNHANDLED;
		}
		
		String path = PathUtil.parseFilePathOrUri(req.getUri()).toString();
		
		if( "GET".equals(req.getVerb()) ) {
			File f = new File(path);
			if( f.exists() ) {
				BaseResponse res = new BaseResponse();
				res.content = FileUtil.getContentCouchObject(f);
				return res;
			} else {
				return new BaseResponse(Response.STATUS_DOESNOTEXIST, "File not found: " + path);
			}
		} else if( "PUT".equals(req.getVerb()) ) {
			File f = new File(path);
			Blob blobToWrite = BlobUtil.getBlob(req.getContent());
			if( ValueUtil.getBoolean(req.getMetadata().get(CcouchNamespace.CCOUCH_RRA_HARDLINK_DESIRED), false) ) {
				BlobUtil.linkBlobToFile(blobToWrite, f);
			} else {
				BlobUtil.writeBlobToFile(blobToWrite, f);
			}
			return new BaseResponse();
		} else {
			return BaseResponse.RESPONSE_UNHANDLED;
		}
	}

}
