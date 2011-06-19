package contentcouch.app.servlet2.comp;

import java.util.HashMap;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.base.BaseResponse;
import contentcouch.blob.Blob;
import contentcouch.misc.MetadataUtil;
import contentcouch.rdf.DcNamespace;

public class RawResourceComponent extends ResourceExplorerComponent {
	protected Map defaultifyConfig( Map config ) {
		config = new HashMap(config);
		if( config.get("title") == null ) config.put("title", "Photo Album Viewer");
		if( config.get("path") == null ) config.put("path", "x-internal:album");
		return config;
	}
	
	public RawResourceComponent( Map config ) {
		super(config);
	}
	
	public Map getProperties() {
		return properties;
	}
	
	protected Response explorifyBlob( Request req, Response subRes, Blob blob ) {
		String type = MetadataUtil.getContentType(subRes, req.getResourceName());
		if( type != null ) {
			BaseResponse res = new BaseResponse(subRes);
			res.putContentMetadata(DcNamespace.DC_FORMAT, type);
			return res;
		} else {
			return subRes;
		}
	}
}
