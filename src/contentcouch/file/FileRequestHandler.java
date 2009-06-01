package contentcouch.file;

import java.io.File;
import java.util.Date;

import togos.rra.BaseRequestHandler;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.blob.BlobUtil;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.repository.TheIdentifier;
import contentcouch.value.Blob;

public class FileRequestHandler extends BaseRequestHandler {

	protected Response put( Request req, File dest, Blob blob, String mergeMethod ) {
		if( dest.exists() ) {
			// TODO: Mind some kind of merge-type metadata.
			if( mergeMethod == null || mergeMethod.equals(CcouchNamespace.RR_FILEMERGE_FAIL) ) {
				throw new RuntimeException( "Cannot PUT at " + req.getUri() + "; file already exists" );
			} else if( mergeMethod.equals(CcouchNamespace.RR_FILEMERGE_IGNORE ) ) {
				return new BaseResponse(Response.STATUS_NORMAL, null);
			} else if( mergeMethod.equals(CcouchNamespace.RR_FILEMERGE_REPLACE ) ) {
				if( !dest.delete() ) {
					throw new RuntimeException( "Could not delete " + req.getUri() + " to replace it.");
				}
			} else if( mergeMethod.startsWith(CcouchNamespace.RR_FILEMERGE_IFSAME ) ) {
				String[] options = mergeMethod.substring(CcouchNamespace.RR_FILEMERGE_IFSAME.length()).split(":");
				if( options.length != 2 ) {
					throw new RuntimeException( "IfSame merge method must be of the form IfSame?<then>:<otherwise>");
				}
				String oldId = TheIdentifier.identify(BlobUtil.getBlob(dest));
				String newId = TheIdentifier.identify(BlobUtil.getBlob(req.getContent()));
				if( oldId.equals(newId) ) {
					mergeMethod = options[0]; 
				} else {
					mergeMethod = options[1];
				}
				return put( req, dest, blob, mergeMethod );
			} else {
				throw new RuntimeException( "Unrecognised merge method: " + mergeMethod );
			}
		}
		
		Blob blobToWrite = BlobUtil.getBlob(blob);
		if( ValueUtil.getBoolean(req.getMetadata().get(CcouchNamespace.RR_HARDLINK_DESIRED), false) ) {
			BlobUtil.linkBlobToFile(blobToWrite, dest);
		} else {
			BlobUtil.writeBlobToFile(blobToWrite, dest);
			Date lastModified = MetadataUtil.getLastModified(req);
			if( lastModified != null ) {
				dest.setLastModified(lastModified.getTime());
			}
		}
		return new BaseResponse();
	}
	
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
			Object content = req.getContent();
			if( (!f.exists() || f.isFile()) && (content instanceof Blob || content instanceof byte[] || content instanceof String ) ) {
				String mergeMethod = ValueUtil.getString(req.getMetadata().get(CcouchNamespace.RR_FILEMERGE_METHOD));
				return put( req, f, (Blob)req.getContent(), mergeMethod );
			} else {
				// TODO: handle merging directories
				throw new RuntimeException("I don't merge dirs!");
			}
		} else {
			return BaseResponse.RESPONSE_UNHANDLED;
		}
	}

}
