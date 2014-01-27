package contentcouch.app.help;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
