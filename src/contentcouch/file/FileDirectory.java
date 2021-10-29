package contentcouch.file;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import togos.mf.api.RequestVerbs;
import togos.mf.base.BaseRequest;
import contentcouch.blob.Blob;
import contentcouch.blob.BlobUtil;
import contentcouch.directory.DirectoryMerger;
import contentcouch.directory.DirectoryMerger.RegularConflictResolver;
import contentcouch.directory.WritableDirectory;
import contentcouch.framework.TheGetter;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class FileDirectory extends File implements WritableDirectory
{
	private static final long serialVersionUID = 1L;
	
	/** Should it try to use hardlinks when writing files? */
	public boolean shouldUseHardlinks;
	
	public class Entry extends File implements Directory.Entry {
		private static final long serialVersionUID = 1L;
		
		public Entry(File file) {
			super(file.getPath());
		}

		////
		
		public String getName() {
			return super.getName();
		}
		
		public long getLastModified() {
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
				return CCouchNamespace.TT_SHORTHAND_DIRECTORY;
			} else {
				return CCouchNamespace.TT_SHORTHAND_BLOB;
			}
		}
		
		////
		
		public void setTargetLastModified(long time) {
			if( time > 0 ) this.setLastModified(time);
		}
		
		public void setTarget(Object value, Map requestMetadata) {
			//assert value != null;
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
				BaseRequest getReq = new BaseRequest( RequestVerbs.GET, sourceUri );
				getReq.metadata = requestMetadata;
				value = TheGetter.getResponseValue(TheGetter.call(getReq), getReq);
				if( value == null ) {
					throw new RuntimeException("Couldn't get blob for "+sourceUri);
				}
			} else {
				sourceUri = "x-undefined:source";
			}
			
			if( value instanceof Directory ) {
				FileUtil.mkdirs(this);
				final RegularConflictResolver cr = new RegularConflictResolver(requestMetadata);
				new DirectoryMerger( cr, requestMetadata ).putAll(getTargetDirectory(), (Directory)value, sourceUri, PathUtil.maybeNormalizeFileUri(getPath()) );
				return;
			}
			
			Blob blob = BlobUtil.getBlob(value);
			if( shouldUseHardlinks ) {
				BlobUtil.linkBlobToFile(blob, this);
			} else {
				BlobUtil.writeBlobToFileAtomically(blob, this);
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
			if( isHidden(subFile) ) continue;
			entries.add(new Entry(subFile));
		}
		return entries;
	}
	
	public Directory.Entry getDirectoryEntry(String key) {
		File f = new File(this.getPath() + "/" + key);
		if( !f.exists() ) return null;
		return new Entry(f);
	}
	
	protected boolean isHiddenFilename( String fn ) {
		return fn.startsWith(".");
	}
	
	protected boolean isHidden( File f ) {
		return f.isHidden() || isHiddenFilename(f.getName());
	}
	
	protected boolean isHidden( Directory.Entry e ) {
		return isHiddenFilename(e.getName());
	}
	
	public void addDirectoryEntry( Directory.Entry entry, Map options ) {
		File f = new File(this.getPath() + "/" + entry.getName());
		//if( f.exists() ) throw new RuntimeException("Cannot add entry; file already exists at " + this + "/" + entry.getName());
		Entry e = new Entry(f);
		e.setTarget(entry.getTarget(), options);
		e.setTargetLastModified(entry.getLastModified());
		if( !isHidden(entry) ) {
			Toucher.touch( this, System.currentTimeMillis(), true, false );
		}
	}
	
	public void deleteDirectoryEntry( String name, Map options ) {
		File f = new File(this.getPath() + "/" + name);
		boolean hidden = isHidden(f);
		FileUtil.rmdir(f);
		if( !hidden ) Toucher.touch( this, System.currentTimeMillis(), true, false );
	}
}
