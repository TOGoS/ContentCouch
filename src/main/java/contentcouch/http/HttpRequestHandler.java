package contentcouch.http;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Date;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.app.Log;
import contentcouch.blob.FileCacheBlob;
import contentcouch.blob.InputStreamBlob;
import contentcouch.framework.BaseRequestHandler;
import contentcouch.rdf.DcNamespace;

public class HttpRequestHandler extends BaseRequestHandler {
	public Response call(Request req) {
		if( !req.getResourceName().startsWith("http://") && !req.getResourceName().startsWith("https://") ) return BaseResponse.RESPONSE_UNHANDLED;

		if( !"GET".equals(req.getVerb()) ) {
			throw new RuntimeException("HTTP handler only does GETs, for now.");
		}
		
		URL url;
		try {
			url = new URL(req.getResourceName());
		} catch( MalformedURLException e ) {
			throw new RuntimeException(e);
		}
		try {
			/*
			HttpGet httpget = new HttpGet(identifier);
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response = httpclient.execute(httpget);
			//return response.getStatusLine();
			HttpEntity httpentity = response.getEntity();
			if( httpentity == null ) return BaseResponse.RESPONSE_UNHANDLED; // Maybe should throw exception, instead?
			long length = httpentity.getContentLength();
			InputStream is = httpentity.getContent();
			return new InputStreamBlob(is, length);
			 */
			
			URLConnection urlConn = url.openConnection();
			long length = urlConn.getContentLength();
			File tempFile = File.createTempFile("httpdownload", null);
			FileCacheBlob fcb = new FileCacheBlob(tempFile, new InputStreamBlob(urlConn.getInputStream(), length));			
			Log.log(Log.EVENT_DOWNLOAD_STARTED, req.getResourceName(), String.valueOf(length) );
			BaseResponse res = new BaseResponse(ResponseCodes.NORMAL, fcb);
			if( urlConn.getLastModified() > 0 ) {
				res.putContentMetadata(DcNamespace.DC_MODIFIED, new Date(urlConn.getLastModified()));
			}
			if( urlConn.getContentType() != null ) {
				res.putContentMetadata(DcNamespace.DC_FORMAT, urlConn.getContentType());
			}
			return res;
		} catch( UnknownHostException e ) {
			String mess = "Unknown host " + url.getHost();
			Log.log(Log.EVENT_WARNING, mess);
			return new BaseResponse(ResponseCodes.DOES_NOT_EXIST, mess, "text/plain");
		} catch( NoRouteToHostException e ) {
			String mess = "No route to host " + url.getHost();
			Log.log(Log.EVENT_WARNING, mess);
			return new BaseResponse(ResponseCodes.DOES_NOT_EXIST, mess, "text/plain");
		} catch( ConnectException e ) {
			String mess = "Could not connect to " + url.getHost() + ":" + url.getPort();
			Log.log(Log.EVENT_WARNING, mess);
			return new BaseResponse(ResponseCodes.DOES_NOT_EXIST, mess, "text/plain");
		} catch( FileNotFoundException e ) {
			return new BaseResponse(ResponseCodes.DOES_NOT_EXIST, "File not found: " + req.getResourceName(), "text/plain");
		} catch( IOException e ) {
			String mess = "I/O error reading " + req.getResourceName() + ": " + e.getMessage();
			if( e.getMessage().startsWith("Server returned HTTP response code: 403") ) {
				// Maybe worth noting, though in many cases this is expected
				Log.log(Log.EVENT_WARNING, mess);
			} else {
				// Anything else, print out.  Maybe I'll add another rule to ignore it, later.
				e.printStackTrace();
				Log.log(Log.EVENT_WARNING, mess);
			}
			return new BaseResponse(ResponseCodes.DOES_NOT_EXIST, mess, "text/plain");
		}
	}

}
