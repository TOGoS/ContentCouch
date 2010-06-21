package contentcouch.merge;

import contentcouch.rdf.CCouchNamespace;

public class FileAdd extends FileChange
{
	protected Object target;
	protected String targetType;
	protected long lastModified;
	
	public FileAdd( String path, Object blob, String targetType, long lastModified, FileChange prev ) {
		super( path, prev );
		this.target = blob;
		this.targetType = targetType;
		this.lastModified = lastModified;
	}

	public FileAdd( String path, Object blob, long lastModified, FileChange prev ) {
		this( path, blob, CCouchNamespace.TT_SHORTHAND_BLOB, lastModified, prev );
	}
	
	public Object getTarget() {
		return target;
	}
	
	public String getTargetType() {
		return targetType;
	}
	
	public long getLastModified() {
		return lastModified;
	}
}
