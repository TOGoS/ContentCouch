package contentcouch.graphics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import contentcouch.directory.SimpleDirectory;
import contentcouch.directory.TransformDirectory;
import contentcouch.misc.UriUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.BaseRef;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class ThumbnailDirectory extends TransformDirectory
{
	public ThumbnailDirectory( Ref back ) {
		super( back );
	}
	public ThumbnailDirectory( Object back, String backUri ) {
		super( back, backUri );
	}
	
	protected static String getThumbnailUri( String imageUri, int width, int height ) {
		return
			"active:contentcouch.graphics.thumbnail+operand@" + UriUtil.uriEncode(imageUri) +
			"+width@data:,"+width+"+height@data:,"+height;
	}
	
	protected void addEntry( Map entryMap, String name, Object target, boolean targetIsDirectory ) {
		Entry newEntry = new SimpleDirectory.Entry( name, target,
			targetIsDirectory ? CcouchNamespace.TT_SHORTHAND_DIRECTORY : CcouchNamespace.TT_SHORTHAND_BLOB );
		entryMap.put( name, newEntry );
	}
	
	protected void addThumbnailEntry( Map entryMap, Entry e, int width, int height ) {
		addEntry( entryMap, e.getName()+"-"+width+"x"+height+".jpg", new BaseRef(getThumbnailUri(getResourceUri(e), width, height)), false );
	}
	
	protected Pattern IMGFNPAT = Pattern.compile(".*?\\.(?:jpe?g|gif|png|bmp)$", Pattern.CASE_INSENSITIVE);
	
	protected Map generateEntryMap() {
		HashMap entryMap = new HashMap();
		for( Iterator i=getBackingDirectory().getDirectoryEntrySet().iterator(); i.hasNext(); ) {
			Directory.Entry be = (Directory.Entry)i.next();
			if( CcouchNamespace.TT_SHORTHAND_DIRECTORY.equals(be.getTargetType()) ) {
				addEntry( entryMap, be.getName(), new ThumbnailDirectory( be.getTarget(), getResourceUri(be) ), true );
			} else {
				if( IMGFNPAT.matcher(be.getName()).matches() ) {
					addThumbnailEntry( entryMap, be, 128, 128 );
					addThumbnailEntry( entryMap, be, 640, 480 );
				}
				entryMap.put( be.getName(), be );
			}
		}
		return entryMap;
	}
}
