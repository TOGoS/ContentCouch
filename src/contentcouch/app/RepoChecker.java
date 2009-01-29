package contentcouch.app;

import java.io.File;

import org.bitpedia.util.Base32;

import contentcouch.data.FileBlob;
import contentcouch.digest.DigestUtil;

public class RepoChecker {
	public String getProperName(File f) {
		byte[] digest = DigestUtil.sha1DigestBlob(new FileBlob(f));
		return Base32.encode(digest);
	}
	
	public void fileFoundOk(File f) {
	}
	
	public void fileFoundInvalid(File f, String b32sha1) {
		System.err.println("INVALID: " + f.getName() + "'s sha-1 sum does not match: " + b32sha1);
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
