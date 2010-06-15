package contentcouch.merge;

public class DirDelete extends FileChange {
	public DirDelete( String path, FileChange prev ) {
		super( path, prev );
	}
}
