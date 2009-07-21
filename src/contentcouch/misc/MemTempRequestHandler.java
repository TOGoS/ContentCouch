package contentcouch.misc;

import contentcouch.directory.WritableDirectory;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Directory;
import togos.rra.BaseRequestHandler;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;

public class MemTempRequestHandler extends BaseRequestHandler {
	protected static Object root = new SimpleDirectory();

	public Response handleRequest(Request req) {
		String path = req.getUri();
		if( !path.startsWith("x-memtemp:") ) return BaseResponse.RESPONSE_UNHANDLED;
		path = path.substring("x-memtemp:".length());
		while( path.startsWith("/") ) path = path.substring(1);
		path = "memtemproot/" + path;
		String[] parts = path.split("/+");
		
		if( Request.VERB_GET.equals(req.getVerb()) ) {
			return get( parts );
		} else if( Request.VERB_PUT.equals(req.getVerb()) ) {
			if( path.length() == 0 ) {
				return new BaseResponse( Response.STATUS_USERERROR, "Cannot PUT at " + req.getUri(), "text/plain");
			}
			return put( parts, req.getContent() );
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
				if( e == null ) return new BaseResponse( Response.STATUS_DOESNOTEXIST, part + " not found in " + parts[i-1], "text/plain");
				obj = e.getTarget();
			} else {
				return new BaseResponse( Response.STATUS_DOESNOTEXIST, parts[i-1] + " not a directory", "text/plain");
			}
		}
		return new BaseResponse(Response.STATUS_NORMAL, obj);
	}

	public Response put( String[] parts, Object newObj ) {
		Object obj = root;
		int i;
		for( i=1; i<parts.length-1; ++i ) {
			String part = parts[i];
			if( obj instanceof Directory ) {
				Directory.Entry e = ((Directory)obj).getDirectoryEntry(part);
				if( e == null ) {
					((WritableDirectory)obj).addDirectoryEntry(new SimpleDirectory.Entry(part, obj = new SimpleDirectory(), CcouchNamespace.OBJECT_TYPE_DIRECTORY));
				} else {
					obj = e.getTarget();
				}
			} else {
				return new BaseResponse( Response.STATUS_DOESNOTEXIST, parts[i-1] + " not a directory", "text/plain");
			}
		}

		String part = parts[i];
		if( obj instanceof Directory ) {
			((WritableDirectory)obj).addDirectoryEntry(new SimpleDirectory.Entry(part, newObj, newObj instanceof Directory ? CcouchNamespace.OBJECT_TYPE_DIRECTORY : CcouchNamespace.OBJECT_TYPE_BLOB));
		} else {
			return new BaseResponse( Response.STATUS_DOESNOTEXIST, parts[i-1] + " not a directory", "text/plain");
		}
		return new BaseResponse(Response.STATUS_NORMAL, "Put object at .../" + part);
	}

}
