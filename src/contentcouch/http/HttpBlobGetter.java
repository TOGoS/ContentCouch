package contentcouch.http;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import contentcouch.app.Log;
import contentcouch.blob.FileCacheBlob;
import contentcouch.blob.InputStreamBlob;
import contentcouch.rdf.RdfNamespace;
import contentcouch.store.Getter;

public class HttpBlobGetter implements Getter {
	public Object get(String identifier) {
		if( !identifier.startsWith("http://") && !identifier.startsWith("https://") ) return null;

		URL url;
		try {
			url = new URL(identifier);
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
			if( httpentity == null ) return null; // Maybe should throw exception, instead?
			long length = httpentity.getContentLength();
			InputStream is = httpentity.getContent();
			return new InputStreamBlob(is, length);
			 */
			
			URLConnection urlConn = url.openConnection();
			long length = urlConn.getContentLength();
			File tempFile = File.createTempFile("httpdownload", null);
			FileCacheBlob fcb = new FileCacheBlob(tempFile, new InputStreamBlob(urlConn.getInputStream(), length));
			if( urlConn.getLastModified() > 0 ) { 
				fcb.putMetadata(RdfNamespace.DC_MODIFIED, new Date(urlConn.getLastModified()));
			}
			return fcb;
		} catch( ConnectException e ) {
			Log.log(Log.LEVEL_WARNINGS, Log.TYPE_NOTFOUND, "Could not connect to " + url.getHost() + ":" + url.getPort());
			return null;
		} catch( FileNotFoundException e ) {
			return null;
		} catch( IOException e ) {
			e.printStackTrace();  // eh
			return null;
		}
	}

}
