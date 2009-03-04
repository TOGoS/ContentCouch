package contentcouch.file;

import java.io.File;


public class FileUtil {
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
}
