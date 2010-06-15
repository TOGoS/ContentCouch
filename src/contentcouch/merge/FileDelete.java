package contentcouch.merge;

public class FileDelete extends FileChange {
	public FileDelete( String path, FileChange prev ) {
		super( path, prev );
	}
}
