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
		String otherPath = ((FileChange)other).getPath();
		if( this instanceof DirDelete && otherPath.startsWith(path+"/")) {
			return 1;
		} else if( other instanceof DirDelete && path.startsWith(otherPath+"/")) {
			return -1;
		}
		return path.compareTo( otherPath );
	}
}
