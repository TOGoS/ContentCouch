package contentcouch.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import contentcouch.blob.BlobUtil;
import contentcouch.file.FileUtil;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.UriUtil;
import contentcouch.rdf.RdfNamespace;
import contentcouch.value.Blob;

public class FileBlobMap implements PutterGetter, StoreFileGetter {
	protected String filenamePrefix;
	public boolean overwriteExistingFiles = false;
	
	public FileBlobMap( String filenamePrefix ) {
		this.filenamePrefix = filenamePrefix;
	}
	
	protected String resolvePath( String pathOrUri ) {
		if( pathOrUri.startsWith("//") || pathOrUri.startsWith("\\\\") ) {
			return pathOrUri;
		} else if( pathOrUri.startsWith("file://") ) {
			String localPart = pathOrUri.substring(7);
			if( localPart.matches("^/[A-Za-z]:.*")) {
				// Windows path
				return UriUtil.uriDecode(localPart.substring(1));
			} else if( localPart.startsWith("/") ) {
				// Unix path
				return localPart;
			} else {
				// Includes host
				return UriUtil.uriDecode("//" + localPart); 
			}
		} else if( pathOrUri.startsWith("file:") ) {
			return filenamePrefix + UriUtil.uriDecode(pathOrUri.substring(5));
		} else {
			return filenamePrefix + pathOrUri;
		}
	}
	
	public File getStoreFile( String filename ) {
		return new File(resolvePath(filename));
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
