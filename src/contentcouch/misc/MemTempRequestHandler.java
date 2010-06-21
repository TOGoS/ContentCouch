package contentcouch.misc;

import java.util.Map;

import contentcouch.directory.SimpleDirectory;
import contentcouch.directory.WritableDirectory;
import contentcouch.framework.BaseRequestHandler;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Directory;
import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;

public class MemTempRequestHandler extends BaseRequestHandler {
	protected static Object root = new SimpleDirectory();

	public Response call(Request req) {
		String path = req.getResourceName();
		if( !path.startsWith("x-memtemp:") ) return BaseResponse.RESPONSE_UNHANDLED;
		path = path.substring("x-memtemp:".length());
		while( path.startsWith("/") ) path = path.substring(1);
		path = "memtemproot/" + path;
		String[] parts = path.split("/+");
		
		if( RequestVerbs.VERB_GET.equals(req.getVerb()) ) {
			return get( parts );
		} else if( RequestVerbs.VERB_PUT.equals(req.getVerb()) ) {
			if( path.length() == 0 ) {
				return new BaseResponse( ResponseCodes.RESPONSE_CALLER_ERROR, "Cannot PUT at " + req.getResourceName(), "text/plain");
			}
			return put( parts, req.getContent(), req.getMetadata() );
		} else {
			return BaseResponse.RESPONSE_UNHANDLED;
		}
	}

	public Response get( String[] parts ) {
		Object obj = root;
		for( int i=1; i<parts.length; ++i ) {
			String part = parts[i];
			if( obj instanceof Directory ) {
				Directory.Entry e = ((Directory)obj).getDirectoryEntry(part);
				if( e == null ) return new BaseResponse( ResponseCodes.RESPONSE_DOESNOTEXIST, part + " not found in " + parts[i-1], "text/plain");
				obj = e.getTarget();
			} else {
				return new BaseResponse( ResponseCodes.RESPONSE_DOESNOTEXIST, parts[i-1] + " not a directory", "text/plain");
			}
		}
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, obj);
	}

	protected Response put( String[] parts, Object newObj, Map requestMetadata ) {
		Object obj = root;
		int i;
		for( i=1; i<parts.length-1; ++i ) {
			String part = parts[i];
			if( obj instanceof Directory ) {
				Directory.Entry e = ((Directory)obj).getDirectoryEntry(part);
				if( e == null ) {
					((WritableDirectory)obj).addDirectoryEntry(new SimpleDirectory.Entry(part, obj = new SimpleDirectory(), CcouchNamespace.TT_SHORTHAND_DIRECTORY), requestMetadata);
				} else {
					obj = e.getTarget();
				}
			} else {
				return new BaseResponse( ResponseCodes.RESPONSE_DOESNOTEXIST, parts[i-1] + " not a directory", "text/plain");
			}
		}

		String part = parts[i];
		if( obj instanceof Directory ) {
			((WritableDirectory)obj).addDirectoryEntry(new SimpleDirectory.Entry(part, newObj, newObj instanceof Directory ? CcouchNamespace.TT_SHORTHAND_DIRECTORY : CcouchNamespace.TT_SHORTHAND_BLOB), requestMetadata);
		} else {
			return new BaseResponse( ResponseCodes.RESPONSE_DOESNOTEXIST, parts[i-1] + " not a directory", "text/plain");
		}
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, "Put object at .../" + part);
	}

}
