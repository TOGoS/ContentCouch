package contentcouch.app.help;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import contentcouch.framework.TheGetter;
import contentcouch.misc.ValueUtil;
import contentcouch.stream.StreamUtil;

public class ContentCouchCommandHelp {
	public static byte[] getData(String name) {
		InputStream is = ContentCouchCommandHelp.class.getResourceAsStream(name+".txt");
		if( is == null ) return null;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			StreamUtil.copyInputToOutput( is, os );
		} catch( IOException e ) {
			throw new RuntimeException( "Error while trying to read help text, ack!", e );
		}
		return os.toByteArray();
	}
	
	public static String getString(String name) {
		String s = ValueUtil.getString(getData(name));
		if( s != null ) s = s.trim();
		return s;
	}
}
