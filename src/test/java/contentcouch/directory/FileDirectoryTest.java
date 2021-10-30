package contentcouch.directory;

import java.io.File;

import contentcouch.file.FileDirectory;
import contentcouch.file.FileUtil;

public class FileDirectoryTest extends WritableDirectoryTest
{
	File fdir = new File("junk/filedirtest");
	
	public void setUp() {
		FileUtil.rmdir(fdir);
		fdir.mkdirs();
		dir = new FileDirectory(fdir);
	}
	
	public void tearDown() {
		FileUtil.rmdir(fdir);
	}
}
