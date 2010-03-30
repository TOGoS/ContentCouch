package contentcouch.app.servlet2.comp;

import java.util.HashMap;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import contentcouch.explorify.AlbumPage;
import contentcouch.value.Directory;

public class PhotoAlbumComponent extends ResourceExplorerComponent {
	protected Map defaultifyConfig( Map config ) {
		config = new HashMap(config);
		if( config.get("title") == null ) config.put("title", "Photo Album Viewer");
		if( config.get("path") == null ) config.put("path", "x-internal:album");
		return config;
	}
	
	public PhotoAlbumComponent( Map config ) {
		super(config);
	}
	
	public Map getProperties() {
		return properties;
	}
	
	public Response explorifyDirectory(Request req, Directory d ) {
		return getPageGeneratorResult(new AlbumPage.AlbumPageGenerator(d, req));
	}
}
