package contentcouch.directory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import contentcouch.framework.TheGetter;
import contentcouch.misc.UriUtil;
import contentcouch.path.PathUtil;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public abstract class TransformDirectory implements Directory {
	protected Object backing;
	protected String backingUri;
	protected Directory backingDirectory;
	protected Map entryMap;
	
	public TransformDirectory( Ref backing ) {
		this.backing = backing;
		this.backingUri = backing.getTargetUri();
	}
	
	public TransformDirectory( Object backing, String backingUri ) {
		this.backing = backing;
	}
	
	protected abstract Map generateEntryMap();
	
	protected String getResourceUri( Directory.Entry e ) {
		String href;
		if( e.getTarget() instanceof Ref ) {
			href = ((Ref)e.getTarget()).getTargetUri();
		} else {
			href = PathUtil.appendPath(backingUri, UriUtil.uriEncode(e.getName()), false);
		}
		return href;
	}
	
	protected Directory getBackingDirectory() {
		if( backing instanceof Directory ) {
			backingDirectory = (Directory)backing;
		} else if( backing instanceof Ref ) {
			backingDirectory = (Directory)TheGetter.get(((Ref)backing).getTargetUri());
			if( backingDirectory == null ) {
				throw new RuntimeException("Backing directory "+((Ref)backing).getTargetUri()+" not found");
			}
		} else {
			throw new RuntimeException("Backing directory was not a Directory or a Ref: " + backing);
		}
		return backingDirectory;
	}
	
	protected Map getEntryMap() {
		if( entryMap == null ) {
			entryMap = generateEntryMap();
		}
		return entryMap;
	}
	
	//// Directory implementation ////
	
	public Set getDirectoryEntrySet() {
		return new HashSet(getEntryMap().values());
	}
	
	public Directory.Entry getDirectoryEntry(String name) {
		return (Directory.Entry)getEntryMap().get(name);
	}
}
