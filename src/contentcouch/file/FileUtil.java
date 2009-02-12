package contentcouch.file;

import java.io.File;


public class FileUtil {
	public static void mkdirs(File d) {
		if( d != null && !d.exists() ) {
			if( !d.mkdirs() ) {
				throw new RuntimeException("Couldn't create dir " + d);
			}
		}
	}
	
	public static void mkParentDirs(File f) {
		File d = f.getParentFile();
		if( d != null && !d.exists() ) {
			if( !d.mkdirs() ) {
				throw new RuntimeException("Couldn't create parent dir for " + f);
			}
		}
	}
	
	public static Object getContentCouchObject(File f) {
		if( f.isDirectory() ) {
			return new FileDirectory(f);
		} else {
			return new FileBlob(f);
		}
	}
}
