package contentcouch.app;

import java.io.File;

import org.bitpedia.util.Base32;

import contentcouch.digest.DigestUtil;
import contentcouch.file.FileBlob;

public class RepoChecker {
	public String getProperName(File f) {
		byte[] digest = DigestUtil.sha1DigestBlob(new FileBlob(f));
		return Base32.encode(digest);
	}
	
	public void fileFoundOk(File f) {
	}
	
	public void fileFoundInvalid(File f, String b32sha1) {
		System.err.println("INVALID: " + f.getPath() + "'s sha-1 sum does not match filename.  Calculated " + b32sha1 +".  Deleting.");
		f.delete();
	}
	
	public void checkFile(File f) {
		f.setReadOnly();
		String properName = getProperName(f);
		if( properName.equals(f.getName()) ) {
			fileFoundOk(f);
		} else {
			fileFoundInvalid(f, properName);
		}
	}
	
	public void checkFiles(File f) {
		if( !f.exists() ) {
			System.err.println("Nothing to check at " + f.getPath());
			return;
		}
		if( f.isDirectory() ) {
			File[] subFiles = f.listFiles();
			for( int i=0; i<subFiles.length; ++i ) {
				File subFile = subFiles[i];
				if( subFile.getName().startsWith(".") ) continue;
				checkFiles( subFile );
			}
		} else {
			checkFile(f);
		}
	}
}
