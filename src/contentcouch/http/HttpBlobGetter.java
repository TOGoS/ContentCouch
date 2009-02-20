package contentcouch.http;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import contentcouch.blob.FileCacheBlob;
import contentcouch.blob.InputStreamBlob;
import contentcouch.rdf.RdfNamespace;
import contentcouch.store.Getter;

public class HttpBlobGetter implements Getter {
	public Object get(String identifier) {
		if( !identifier.startsWith("http:") && !identifier.startsWith("https:") ) return null;

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
			
			URLConnection urlConn = new URL(identifier).openConnection();
			long length = urlConn.getContentLength();
			System.err.println("Downloading " + identifier + " (" + length + " bytes)");
			File tempFile = File.createTempFile("httpdownload", null);
			FileCacheBlob fcb = new FileCacheBlob(tempFile, new InputStreamBlob(urlConn.getInputStream(), length));
			if( urlConn.getLastModified() > 0 ) { 
				fcb.putMetadata(RdfNamespace.DC_MODIFIED, new Date(urlConn.getLastModified()));
			}
			return fcb;
		} catch( FileNotFoundException e ) {
			return null;
		} catch( IOException e ) {
			e.printStackTrace();  // eh
			return null;
		}
	}

}
