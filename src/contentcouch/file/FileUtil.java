package contentcouch.file;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import contentcouch.stream.StreamUtil;

public class FileUtil
{
	public static void rmdir(File f) {
		if( !f.exists() ) return; 
		if( f.isDirectory() ) {
			File[] fs = f.listFiles();
			for( int i=fs.length-1; i>=0; --i ) {
				rmdir(fs[i]);
			}
		}
		f.delete();
	}
	
	public static boolean mkdirs(File d) {
		if( d != null && !d.exists() ) {
			if( !d.mkdirs() ) {
				throw new RuntimeException("Couldn't create dir " + d);
			}
			return true;
		}
		return false;
	}
	
	public static boolean mkParentDirs(File f) {
		File d = f.getParentFile();
		return mkdirs(d);
	}
	
	public static Object getContentCouchObject(File f) {
		if( f.isDirectory() ) {
			return new FileDirectory(f);
		} else {
			return new FileBlob(f);
		}
	}
	
	protected static void reallyClose( Closeable c ) {
		try {
			c.close();
		} catch( IOException e ) {
			// System.err.println("Failed to close some stream.  As if I care.");
		}
	}
	
	public static void copy( File in, File out ) {
		FileInputStream is = null;
		FileOutputStream os = null;
		try {
			is = new FileInputStream(in);
			os = new FileOutputStream(out);
			StreamUtil.copyInputToOutput( is, os );
		} catch( IOException e ) {
			throw new RuntimeException( "Error while copying " + in.getPath() + " to " + out.getPath(), e );
		} finally {
			reallyClose(os);
			reallyClose(is);
		}
	}
}
