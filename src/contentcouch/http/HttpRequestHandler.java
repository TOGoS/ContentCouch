package contentcouch.http;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import togos.rra.BaseRequestHandler;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.app.Log;
import contentcouch.blob.FileCacheBlob;
import contentcouch.blob.InputStreamBlob;
import contentcouch.rdf.DcNamespace;

public class HttpRequestHandler extends BaseRequestHandler {
	public Response handleRequest(Request req) {
		if( !req.getUri().startsWith("http://") && !req.getUri().startsWith("https://") ) return BaseResponse.RESPONSE_UNHANDLED;

		if( !"GET".equals(req.getVerb()) ) {
			throw new RuntimeException("HTTP handler only does GETs, for now.");
		}
		
		URL url;
		try {
			url = new URL(req.getUri());
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
			Log.log(Log.LEVEL_CHATTY, Log.TYPE_DOWNLOADING, req.getUri() + ", " + length + " bytes");
			File tempFile = File.createTempFile("httpdownload", null);
			FileCacheBlob fcb = new FileCacheBlob(tempFile, new InputStreamBlob(urlConn.getInputStream(), length));			
			BaseResponse res = new BaseResponse(Response.STATUS_NORMAL, fcb);
			if( urlConn.getLastModified() > 0 ) {
				res.putContentMetadata(DcNamespace.DC_MODIFIED, new Date(urlConn.getLastModified()));
			}
			if( urlConn.getContentType() != null ) {
				res.putContentMetadata(DcNamespace.DC_FORMAT, urlConn.getContentType());
			}
			return res;
		} catch( NoRouteToHostException e ) {
			String mess = "No route to host " + url.getHost();
			Log.log(Log.LEVEL_WARNINGS, Log.TYPE_NOTFOUND, mess );
			return new BaseResponse(Response.STATUS_DOESNOTEXIST, mess, "text/plain");
		} catch( ConnectException e ) {
			String mess = "Could not connect to " + url.getHost() + ":" + url.getPort();
			Log.log(Log.LEVEL_WARNINGS, Log.TYPE_NOTFOUND, mess);
			return new BaseResponse(Response.STATUS_DOESNOTEXIST, mess, "text/plain");
		} catch( FileNotFoundException e ) {
			return new BaseResponse(Response.STATUS_DOESNOTEXIST, "File not found: " + req.getUri(), "text/plain");
		} catch( IOException e ) {
			e.printStackTrace();  // eh
			String mess = "I/O error reading " + req.getUri();
			Log.log(Log.LEVEL_WARNINGS, Log.TYPE_NOTFOUND, mess);
			return new BaseResponse(Response.STATUS_DOESNOTEXIST, mess, "text/plain");
		}
	}

}
