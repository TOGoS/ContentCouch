package contentcouch.file;

import java.io.File;
import java.util.Date;

import togos.rra.BaseRequestHandler;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.directory.MergeUtil;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.SimpleDirectory;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;

public class FileRequestHandler extends BaseRequestHandler {
	public Response handleRequest( Request req ) {
		if( !req.getUri().startsWith("file:") ) return BaseResponse.RESPONSE_UNHANDLED;
		
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

			File pf = f.getParentFile();
			if( pf == null ) pf = f.getAbsoluteFile().getParentFile();
			if( pf == null ) throw new RuntimeException("No parent of " + f);
			FileUtil.mkdirs(pf);
			FileDirectory destDir = new FileDirectory(pf);
			// TODO: handle RR_REHARDLINK_RESIRED
			destDir.shouldUseHardlinks = ValueUtil.getBoolean(req.getMetadata().get(CcouchNamespace.REQ_HARDLINK_DESIRED), false);
			SimpleDirectory.Entry newEntry = new SimpleDirectory.Entry();
			newEntry.name = f.getName();
			newEntry.target = req.getContent();
			Date lastModified = MetadataUtil.getLastModified(req);
			if( lastModified != null ) {
				newEntry.targetLastModified = lastModified.getTime();
			}
			MergeUtil.put(destDir, newEntry, new MergeUtil.RegularConflictResolver(req.getMetadata()));
			return new BaseResponse();
		} else {
			return BaseResponse.RESPONSE_UNHANDLED;
		}
	}

}
