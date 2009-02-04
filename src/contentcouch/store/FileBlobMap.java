package contentcouch.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import contentcouch.data.Blob;
import contentcouch.data.BlobUtil;
import contentcouch.data.FileBlob;
import contentcouch.file.FileUtil;

public class FileBlobMap implements PutterGetter, FileGetter {
	protected String filenamePrefix;
	public boolean overwriteExistingFiles = false;
	
	public FileBlobMap( String filenamePrefix ) {
		this.filenamePrefix = filenamePrefix;
	}
	
	public File getFile( String filename ) {
		return new File(filenamePrefix + filename);
	}
	
	public void put( String filename, Object obj ) {
		Blob blob = BlobUtil.getBlob(obj);
		File file = getFile( filename );
		// TODO: Have a hardlink option, and if set, use that when blob is another file
		if( !file.exists() || overwriteExistingFiles ) {
			try {
				FileUtil.mkParentDirs( file );
				File tempFile = new File(file.getParent() + "/." + file.getName() + ".cc-temp-" + System.currentTimeMillis() + "-" + (int)(Math.random()*9999));
				FileOutputStream fos = new FileOutputStream(tempFile);
				try {
					long written = 0;
					long length = blob.getLength();
					int chunkLength = 1024*1024;
					while( written < length ) {
						int thisChunkLength = (length - written) > chunkLength ? chunkLength : (int)(length - written);
						fos.write(blob.getData(written, thisChunkLength));
						written += chunkLength;
					}
				} finally {
					fos.close();
				}
				if( file.exists() ) file.delete();
				tempFile.renameTo(file);
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
		}
	}

	public Object get( String filename ) {
		File file = getFile( filename );
		if( file.isDirectory() ) return file;
		if( file.exists() ) return new FileBlob( file );		
		return null;
	}
}
