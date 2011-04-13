package contentcouch.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import contentcouch.blob.BlobUtil;
import contentcouch.blob.ByteArrayBlob;
import contentcouch.framework.BaseRequestHandler;

import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import togos.mf.value.Blob;

public class InternalStreamRequestHandler extends BaseRequestHandler {
	static final InternalStreamRequestHandler instance = new InternalStreamRequestHandler();
	public static InternalStreamRequestHandler getInstance() {
		return instance;
	}
	
	static final String URI_PREFIX = "x-internal-stream:";
	
	public HashMap streams = new HashMap();
	public HashMap cachedInput = new HashMap();
	
	public void addInputStream( String name, InputStream stream ) {
		streams.put(name, stream);
	}
	
	public void addOutputStream( String name, OutputStream stream ) {
		streams.put(name, stream);
	}

	public void putBlob( Blob blob, String streamName ) {
		Object oso = streams.get(streamName);
		if( oso == null ) throw new RuntimeException("No such stream: " + streamName);
		if( !(oso instanceof OutputStream) ) throw new RuntimeException("Not an output stream: " + streamName);
		OutputStream os = (OutputStream)oso;
		try {
			BlobUtil.writeBlobToOutputStream(blob, os);
			os.close();
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public Blob getBlob( String streamName ) {
		Blob blob = (Blob)cachedInput.get(streamName);
		if( blob != null ) return blob;
		Object iso = streams.get(streamName);
		if( iso == null ) throw new RuntimeException("No such stream: " + streamName);
		if( !(iso instanceof InputStream) ) throw new RuntimeException("Not an input stream: " + streamName);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			StreamUtil.copyInputToOutput((InputStream)iso, baos);
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
		blob = new ByteArrayBlob(baos.toByteArray());
		cachedInput.put(streamName, blob);
		return blob;
	}

	public Response call(Request req) {
		if( !req.getResourceName().startsWith(URI_PREFIX) ) return BaseResponse.RESPONSE_UNHANDLED;
		String streamName = req.getResourceName().substring(URI_PREFIX.length());
		if( RequestVerbs.POST.equals(req.getVerb()) || RequestVerbs.PUT.equals(req.getVerb()) ) {
			Object content = req.getContent();
			if( content == null ) {
				throw new RuntimeException("Can't PUT or POST to " + req.getResourceName() + " without content");
			}
			Blob blob = BlobUtil.getBlob(content);
			putBlob(blob, streamName);
			return new BaseResponse(ResponseCodes.NORMAL, "Written");
		} else if( RequestVerbs.GET.equals(req.getVerb()) ) {
			return new BaseResponse(ResponseCodes.NORMAL, getBlob(streamName));
		} else {
			throw new RuntimeException("Don't know how to handle " + req.getVerb() );
		}
	}

}
