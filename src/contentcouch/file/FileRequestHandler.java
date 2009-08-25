package contentcouch.file;

import java.io.File;
import java.util.Date;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.directory.DirectoryMerger;
import contentcouch.framework.BaseRequestHandler;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.SimpleDirectory;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.rdf.DcNamespace;

public class FileRequestHandler extends BaseRequestHandler {
	public Response call( Request req ) {
		if( !req.getResourceName().startsWith("file:") ) return BaseResponse.RESPONSE_UNHANDLED;
		
		String path = PathUtil.parseFilePathOrUri(req.getResourceName()).toString();
		
		if( "GET".equals(req.getVerb()) ) {
			File f = new File(path);
			if( f.exists() ) {
				BaseResponse res = new BaseResponse();
				res.content = FileUtil.getContentCouchObject(f);
				res.putContentMetadata(DcNamespace.DC_MODIFIED, new Date(f.lastModified()));
				return res;
			} else {
				return new BaseResponse(ResponseCodes.RESPONSE_DOESNOTEXIST, "File not found: " + path);
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
			BaseResponse res = new BaseResponse();
			
			DirectoryMerger merger = new DirectoryMerger( new DirectoryMerger.RegularConflictResolver(req.getMetadata()), false );
			
			if( merger.put(destDir, newEntry, MetadataUtil.getSourceUriOrUnknown(req.getContentMetadata()), req.getResourceName()) ) {
				res.putMetadata(CcouchNamespace.RES_DEST_ALREADY_EXISTED, Boolean.TRUE);
			}
			return res;
		} else {
			return BaseResponse.RESPONSE_UNHANDLED;
		}
	}

}
