package contentcouch.data;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import contentcouch.file.FileUtil;
import contentcouch.xml.RDF;

public class FileDirectory implements Directory {
	public static class FileDirectoryEntry implements Directory.Entry {
		protected File file;
		
		public FileDirectoryEntry(File file) {
			this.file = file;
		}

		public long getLastModified() {
			return file.lastModified();
		}

		public String getName() {
			return file.getName();
		}

		public Object getTarget() {
			return FileUtil.getContentCouchObject(file);
		}

		public String getTargetType() {
			if( file.isDirectory() ) {
				return RDF.OBJECT_TYPE_DIRECTORY;
			} else {
				return RDF.OBJECT_TYPE_BLOB;
			}
		}
	}
	
	protected File file;
	
	public FileDirectory( File file ) {
		this.file = file;
	}
	
	public Map getEntries() {
		File[] subFiles = file.listFiles();
		HashMap entries = new HashMap();
		for( int i=0; i<subFiles.length; ++i ) {
			File subFile = subFiles[i];
			entries.put(subFile.getName(), new FileDirectoryEntry(subFile));
		}
		return entries;
	}
}
