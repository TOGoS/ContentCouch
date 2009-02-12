package contentcouch.http;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import contentcouch.blob.InputStreamBlob;
import contentcouch.store.Getter;

public class HttpBlobGetter implements Getter {
	public Object get(String identifier) {
		if( !identifier.startsWith("http:") && !identifier.startsWith("https:") ) return null;
		
		try {
			HttpGet httpget = new HttpGet(identifier);
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response = httpclient.execute(httpget);
			//return response.getStatusLine();
			HttpEntity httpentity = response.getEntity();
			if( httpentity == null ) return null; // Maybe should throw exception, instead?
			long length = httpentity.getContentLength();
			InputStream is = httpentity.getContent();
			return new InputStreamBlob(is, length);
		} catch( IOException e ) {
			e.printStackTrace();  // eh
			return null;
		}
	}

}
