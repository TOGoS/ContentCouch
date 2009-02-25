package contentcouch.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import contentcouch.blob.BlobUtil;
import contentcouch.file.FileUtil;
import contentcouch.misc.MetadataUtil;
import contentcouch.rdf.RdfNamespace;
import contentcouch.value.Blob;

public class FileBlobMap implements PutterGetter, StoreFileGetter {
	protected String filenamePrefix;
	public boolean overwriteExistingFiles = false;
	
	public FileBlobMap( String filenamePrefix ) {
		this.filenamePrefix = filenamePrefix;
	}
	
	public File getStoreFile( String filename ) {
		return new File(filenamePrefix + filename);
	}
	
	public File getStoreFile( Blob blob ) {
		return null;
	}

	public void put( String filename, Object obj ) {
		Blob blob = BlobUtil.getBlob(obj);
		File file = getStoreFile( filename );
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
				Date lm = (Date)MetadataUtil.getMetadataFrom(blob, RdfNamespace.DC_MODIFIED);
				if( lm != null ) file.setLastModified(lm.getTime());
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
		}
	}

	public Object get( String filename ) {
		File file = getStoreFile( filename );
		if( !file.exists() ) return null;
		return FileUtil.getContentCouchObject(file);		
	}
}
