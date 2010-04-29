package contentcouch.file;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import togos.mf.api.RequestVerbs;
import togos.mf.base.BaseRequest;
import togos.mf.value.Blob;

import contentcouch.blob.BlobUtil;
import contentcouch.directory.DirectoryMerger;
import contentcouch.directory.WritableDirectory;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class FileDirectory extends File implements WritableDirectory {
	/** Should it try to use hardlinks when writing files? */
	public boolean shouldUseHardlinks;
	
	public class Entry extends File implements Directory.Entry {
		public Entry(File file) {
			super(file.getPath());
		}

		////
		
		public String getName() {
			return super.getName();
		}
		
		public long getTargetLastModified() {
			return lastModified();
		}

		protected FileDirectory getTargetDirectory() {
			FileDirectory fd = new FileDirectory(this);
			fd.shouldUseHardlinks = shouldUseHardlinks;
			return fd;
		}
		
		public Object getTarget() {
			if( this.isDirectory() ) {
				return getTargetDirectory();
			}
			return FileUtil.getContentCouchObject(this);
		}
		
		public long getTargetSize() {
			if( isFile() ) return length();
			return -1;
		}

		public String getTargetType() {
			if( isDirectory() ) {
				return CcouchNamespace.TT_SHORTHAND_DIRECTORY;
			} else {
				return CcouchNamespace.TT_SHORTHAND_BLOB;
			}
		}
		
		////
		
		public void setTargetLastModified(long time) {
			if( time > 0 ) this.setLastModified(time);
		}
		
		public void setTarget(Object value, Map requestMetadata) {
			if( this.exists() ) {
				if( !this.delete() ) {
					if( this.isDirectory() ) {
						throw new RuntimeException( "Could not delete directory " + this + " to replace.  Ensure that it is empty.");
					} else {
						throw new RuntimeException( "Could not delete " + this + " to replace");
					}
				}
			}
			
			String sourceUri;
			if( value instanceof Ref ) {
				sourceUri = ((Ref)value).getTargetUri();
				BaseRequest getReq = new BaseRequest( RequestVerbs.VERB_GET, sourceUri );
				getReq.metadata = requestMetadata;
				value = TheGetter.getResponseValue(TheGetter.call(getReq), getReq);
			} else {
				sourceUri = "x-undefined:source";
			}
			
			if( value instanceof Directory ) {
				FileUtil.mkdirs(this);
				new DirectoryMerger( null, requestMetadata ).putAll(getTargetDirectory(), (Directory)value, sourceUri, PathUtil.maybeNormalizeFileUri(getPath()) );
				return;
			}
			
			Blob blob = BlobUtil.getBlob(value);
			if( shouldUseHardlinks ) {
				BlobUtil.linkBlobToFile(blob, this);
			} else {
				BlobUtil.writeBlobToFile(blob, this);
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
			if( subFile.isHidden() ) continue;
			entries.add(new Entry(subFile));
		}
		return entries;
	}
	
	public Directory.Entry getDirectoryEntry(String key) {
		File f = new File(this.getPath() + "/" + key);
		if( !f.exists() ) return null;
		return new Entry(f);
	}
	
	public void addDirectoryEntry(Directory.Entry entry, Map requestMetadata) {
		File f = new File(this.getPath() + "/" + entry.getName());
		//if( f.exists() ) throw new RuntimeException("Cannot add entry; file already exists at " + this + "/" + entry.getName());
		Entry e = new Entry(f);
		e.setTarget(entry.getTarget(), requestMetadata);
		e.setTargetLastModified(entry.getTargetLastModified());
	}
}
