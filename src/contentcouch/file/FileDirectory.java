package contentcouch.file;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Directory;

public class FileDirectory extends File implements Directory {
	public static class FileDirectoryEntry extends File implements Directory.Entry {
		public FileDirectoryEntry(File file) {
			super(file.getPath());
		}

		public String getName() {
			return getName();
		}
		
		public long getLastModified() {
			if( isFile() ) return lastModified();
			return -1; // Mtime on a directory doesn't necessarily mean much, so let's ignore
		}

		public Object getValue() {
			return FileUtil.getContentCouchObject(this);
		}
		
		public long getSize() {
			if( isFile() ) return length();
			return -1;
		}

		public String getTargetType() {
			if( isDirectory() ) {
				return CcouchNamespace.OBJECT_TYPE_DIRECTORY;
			} else {
				return CcouchNamespace.OBJECT_TYPE_BLOB;
			}
		}
	}
	
	public FileDirectory( String path ) {
		super(path);
	}
	
	public FileDirectory( File file ) {
		super(file.getPath());
	}
	
	public Set getDirectoryEntrySet() {
		File[] subFiles = listFiles();
		HashSet entries = new HashSet();
		if( subFiles != null ) for( int i=0; i<subFiles.length; ++i ) {
			File subFile = subFiles[i];
			if( subFile.getName().startsWith(".") ) continue;
			entries.add(new FileDirectoryEntry(subFile));
		}
		return entries;
	}
	
	public Entry getDirectoryEntry(String key) {
		File f = new File(this.getPath() + "/" + key);
		if( !f.exists() ) return null;
		return new FileDirectoryEntry(f);
	}
}
