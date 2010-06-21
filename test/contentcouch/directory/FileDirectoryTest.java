package contentcouch.directory;

import java.io.File;

import contentcouch.file.FileDirectory;

public class FileDirectoryTest extends WritableDirectoryTest
{
	File fdir = new File("junk/filedirtest");
	
	protected void rmdir(File f) {
		if( !f.exists() ) return; 
		if( f.isDirectory() ) {
			File[] fs = f.listFiles();
			for( int i=fs.length-1; i>=0; --i ) {
				rmdir(fs[i]);
			}
		}
		f.delete();
	}
	
	public void setUp() {
		rmdir(fdir);
		fdir.mkdirs();
		dir = new FileDirectory(fdir);
	}
	
	public void tearDown() {
		rmdir(fdir);
	}
}
