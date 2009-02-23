package contentcouch.file;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import contentcouch.rdf.RdfNamespace;
import contentcouch.value.Directory;

public class FileDirectory extends File implements Directory {
	public static class FileDirectoryEntry extends File implements Directory.Entry {
		public FileDirectoryEntry(File file) {
			super(file.getPath());
		}

		public long getLastModified() {
			if( isFile() ) return lastModified();
			return -1; // Mtime on a directory doesn't necessarily mean much, so let's ignore
		}

		public Object getTarget() {
			return FileUtil.getContentCouchObject(this);
		}
		
		public long getSize() {
			if( isFile() ) return length();
			return -1;
		}

		public String getTargetType() {
			if( isDirectory() ) {
				return RdfNamespace.OBJECT_TYPE_DIRECTORY;
			} else {
				return RdfNamespace.OBJECT_TYPE_BLOB;
			}
		}
	}
	
	public FileDirectory( File file ) {
		super(file.getPath());
	}
	
	public Map getEntries() {
		File[] subFiles = listFiles();
		HashMap entries = new HashMap();
		for( int i=0; i<subFiles.length; ++i ) {
			File subFile = subFiles[i];
			if( subFile.getName().startsWith(".") ) continue;
			entries.put(subFile.getName(), new FileDirectoryEntry(subFile));
		}
		return entries;
	}
}
