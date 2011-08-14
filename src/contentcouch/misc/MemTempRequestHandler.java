package contentcouch.misc;

import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.directory.SimpleDirectory;
import contentcouch.directory.WritableDirectory;
import contentcouch.framework.BaseRequestHandler;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.value.Directory;

public class MemTempRequestHandler extends BaseRequestHandler {
	protected Object root = new SimpleDirectory();
	
	public Response call(Request req) {
		String path = req.getResourceName();
		if( !path.startsWith("x-memtemp:") ) return BaseResponse.RESPONSE_UNHANDLED;
		path = path.substring("x-memtemp:".length());
		while( path.startsWith("/") ) path = path.substring(1);
		path = "memtemproot/" + path;
		String[] parts = path.split("/+");
		
		if( RequestVerbs.GET.equals(req.getVerb()) ) {
			return get( parts );
		} else if( RequestVerbs.PUT.equals(req.getVerb()) ) {
			if( path.length() == 0 ) {
				return new BaseResponse( ResponseCodes.CALLER_ERROR, "Cannot PUT at " + req.getResourceName(), "text/plain");
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
				if( e == null ) return new BaseResponse( ResponseCodes.DOES_NOT_EXIST, part + " not found in " + parts[i-1], "text/plain");
				obj = e.getTarget();
			} else {
				return new BaseResponse( ResponseCodes.DOES_NOT_EXIST, parts[i-1] + " not a directory", "text/plain");
			}
		}
		return new BaseResponse(ResponseCodes.NORMAL, obj);
	}

	protected Response put( String[] parts, Object newObj, Map requestMetadata ) {
		Object obj = root;
		int i;
		for( i=1; i<parts.length-1; ++i ) {
			String part = parts[i];
			if( obj instanceof Directory ) {
				Directory.Entry e = ((Directory)obj).getDirectoryEntry(part);
				if( e == null ) {
					((WritableDirectory)obj).addDirectoryEntry(new SimpleDirectory.Entry(part, obj = new SimpleDirectory(), CCouchNamespace.TT_SHORTHAND_DIRECTORY), requestMetadata);
				} else {
					obj = e.getTarget();
				}
			} else {
				return new BaseResponse( ResponseCodes.DOES_NOT_EXIST, parts[i-1] + " not a directory", "text/plain");
			}
		}

		String part = parts[i];
		if( obj instanceof Directory ) {
			((WritableDirectory)obj).addDirectoryEntry(new SimpleDirectory.Entry(part, newObj, newObj instanceof Directory ? CCouchNamespace.TT_SHORTHAND_DIRECTORY : CCouchNamespace.TT_SHORTHAND_BLOB), requestMetadata);
		} else {
			return new BaseResponse( ResponseCodes.DOES_NOT_EXIST, parts[i-1] + " not a directory", "text/plain");
		}
		return new BaseResponse(ResponseCodes.NORMAL, "Put object at .../" + part);
	}

}
