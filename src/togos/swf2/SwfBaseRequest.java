package togos.swf2;

import java.util.HashMap;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.base.BaseRequest;

public class SwfBaseRequest extends BaseRequest {
	public SwfBaseRequest( String verb, String uri ) {
		super(verb,uri);
	}
	public SwfBaseRequest( String verb, String uri, Object content, Map contentMetadata ) {
		super(verb,uri,content,contentMetadata);
	}
	public SwfBaseRequest( Request req ) {
		super( req );
	}
	public SwfBaseRequest( Request req, String uri ) {
		super( req, uri );
	}
	
	//// Config
	
	// 'clean' = 'read only' or 'immutable'
	// if clean, then we need to clone before adding to it
	protected boolean configClean = true;
	
	protected Map getConfig() {
		return (Map)getContextVars().get(SwfNamespace.CTX_CONFIG);
	}
	
	protected Map getWritableConfigVars() {
		Map config = getConfig();
		if( config == null ) {
			setConfig(config = new HashMap(), false);
		} else if( configClean ) {
			setConfig(config = new HashMap(config), false);
		}
		return config;
	}
	
	public void setConfig( Map newConfig, boolean clean ) {
		putContextVar(SwfNamespace.CTX_CONFIG, newConfig);
		configClean = clean;
	}
	
	public void putAllConfig( Map newVars, boolean clean ) {
		if( getConfig() == null ) {
			setConfig( newVars, clean );
		} else {
			getWritableConfigVars().putAll(newVars);
		}
	}

	public void putAllConfig( Map newVars ) {
		putAllConfig( newVars, true );
	}
	
	public void putConfig( String key, Object value ) {
		getWritableConfigVars().put(key, value);
	}
}
