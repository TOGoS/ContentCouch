package contentcouch.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import contentcouch.data.Blob;
import contentcouch.data.FileBlob;
import contentcouch.file.FileUtil;

public class FileBlobMap implements BlobMap, FileGetter {
	protected String filenamePrefix;
	
	public FileBlobMap( String filenamePrefix ) {
		this.filenamePrefix = filenamePrefix;
	}
	
	public File getFile( String filename ) {
		return new File(filenamePrefix + filename);
	}
	
	public void put( String filename, Blob blob ) {
		File file = getFile( filename );
		if( !file.exists() ) {
			try {
				FileUtil.mkParentDirs( file );
				FileOutputStream fos = new FileOutputStream(file);
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
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
		}
	}

	public Blob get( String filename ) {
		File file = getFile( filename );
		if( file.exists() ) return new FileBlob( file );		
		return null;
	}
}
