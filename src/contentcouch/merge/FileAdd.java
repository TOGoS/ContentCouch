package contentcouch.merge;

public class FileAdd extends FileChange {
	protected Object blob;

	public FileAdd( String path, Object blob, FileChange prev ) {
		super( path, prev );
		this.blob = blob;
	}
	
	public Object getBlob() {
		return blob;
	}
}
