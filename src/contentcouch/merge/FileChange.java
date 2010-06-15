package contentcouch.merge;

public class FileChange implements Comparable {
	protected String path;
	protected FileChange prev;
	
	public FileChange( String path, FileChange prev ) {
		this.path = path;
		this.prev = prev;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public FileChange getPrev() {
		return prev;
	}
	
	public int compareTo( Object other ) {
		return path.compareTo( ((FileChange)other).getPath() );
	}
}
