package contentcouch.explorify;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import togos.mf.api.Request;
import togos.mf.base.BaseArguments;
import togos.mf.value.Arguments;
import togos.swf2.Component;
import togos.swf2.SwfFrontRequestHandler;
import togos.swf2.SwfNamespace;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.xml.XML;

public class CCouchExplorerPageGenerator extends PageGenerator
{
	public static String ALLOW_RELATIVE_RESOURCE_URIS = "allowRelativeResourceUris";
	public static String PREFER_RELATIVE_RESOURCE_URIS = "preferRelativeResourceUris";
	
	protected Request req;
	protected boolean allowRelativeResourceUris;
	protected boolean preferRelativeResourceUris;
	
	public CCouchExplorerPageGenerator( Request req ) {
		this.req = req;
		this.allowRelativeResourceUris = ValueUtil.getBoolean(getArgument(ALLOW_RELATIVE_RESOURCE_URIS), false);
		this.preferRelativeResourceUris = ValueUtil.getBoolean(getArgument(PREFER_RELATIVE_RESOURCE_URIS), false);
	}
	
	//// Utility functions ////
	
	protected static Arguments parseArgumentsFromUri(String uri) {
		int qidx = uri.indexOf("?");
		if( qidx == -1 ) return new BaseArguments();
		String flaf = uri.substring(qidx+1);
		if( flaf.length() == 0 ) return new BaseArguments();
		
		Map nargs = new HashMap();
		String[] parts = flaf.split("&");
		for( int i=0; i<parts.length; ++i ) {
			if( parts[i] == null ) continue;
			String[] kv = parts[i].split("=",2);
			
			if( kv.length == 1 ) {
				String k = UriUtil.uriDecode(kv[0]);
				nargs.put(k, k);
			} else {
				nargs.put(UriUtil.uriDecode(kv[0]), UriUtil.uriDecode(kv[1]));
			}
		}
		return new BaseArguments(Collections.EMPTY_LIST, nargs);
	}
	
	protected Arguments getArguments() {
		if( req.getContent() instanceof Arguments ) {
			return (Arguments)req.getContent();
		}
		return new BaseArguments();
	}
	
	protected String getArgument( String key ) {
		if( req.getContent() instanceof Arguments ) {
			return ValueUtil.getString(((Arguments)req.getContent()).getNamedArguments().get(key));
		}
		return null;
	}
	
	protected String getOperandUri() {
		return getArgument("uri");
	}
	
	protected Component getSwfComponent() {
		return (Component)req.getContextVars().get(SwfNamespace.COMPONENT);
	}
	
	protected SwfFrontRequestHandler getSwfFront() {
		return (SwfFrontRequestHandler)req.getContextVars().get(SwfNamespace.FRONT);
	}
	
	protected String getExternalComponentUri( Request req, Component component, Arguments args ) {
		return getSwfFront().getExternalComponentUri(req, component, args);
	}
	
	protected String externalUriCache;
	protected String getExternalUri() {
		if( externalUriCache == null ) {
			externalUriCache = getExternalComponentUri(req, getSwfComponent(), getArguments());
		}
		return externalUriCache;
	}
	
	protected String getResourceUri( String path ) {
		if( path.startsWith(SwfNamespace.SERVLET_PATH_URI_PREFIX) ) return path;
		if( path.charAt(0) != '/' ) path = "/" + path;
		return SwfNamespace.SERVLET_PATH_URI_PREFIX + path;
	}
	
	protected String getExternalResourceUri( Request req, String path ) {
		String resourceUri = getResourceUri(path);
		SwfFrontRequestHandler swfFront = getSwfFront();
		if( swfFront != null ) {
			return swfFront.getExternalUri(req, resourceUri);
		} else {
			return getExternalUri("raw", resourceUri);
		}
	}
	
	protected String getName() {
		String name = (String)getArguments().getNamedArguments().get("name");
		return (name == null) ? getOperandUri() : name;
	}
	
	////
	
	protected String getExternalUri( String whichProcessor, String internalUri ) {
		if( internalUri.startsWith(SwfNamespace.SERVLET_PATH_URI_PREFIX)) {
			return getSwfFront().getExternalUri(req, internalUri);
		} else {
			BaseArguments args = new BaseArguments();
			args.putNamedArgument("uri", internalUri);
			return getExternalComponentUri( req, getSwfComponent(), args );
		}
	}
	
	protected String getExternalUri( String whichProcessor, String baseUri, String relativeUri, boolean allowRelative ) {
		if( allowRelative && !PathUtil.isAbsolute(relativeUri) ) {
			return relativeUri;
		} else {
			return getExternalUri( whichProcessor, PathUtil.appendPath(baseUri, relativeUri));
		}
	}
	
	protected String generateScriptInclude( String scriptName ) {
		String ppUri = SwfNamespace.SERVLET_PATH_URI_PREFIX + "/lib/js/"+UriUtil.uriEncode(scriptName);
		ppUri = getExternalUri("raw", ppUri);
		return "<script type=\"application/javascript\" src=\"" + XML.xmlEscapeAttributeValue(ppUri) + "\"></script>";
	}
	
	////
	
	public void writeContent(PrintWriter w) {
		w.println("<p>Nothing to see here.</p>");
	}
	
	protected String getPageShortTitle() {
		return "";
	}
	
	protected String getPageLongTitle() {
		return getPageShortTitle();
	}

	public void write(PrintWriter w) {
		writeHeader(w);
		String st = getPageShortTitle();
		if( st.length() > 0 ) w.println("<h2>" + XML.xmlEscapeText(st) + "</h2>");
		writeContent(w);
		writeFooter(w);
	}
	
	public void writeHeader(PrintWriter w) {
		w.println("<html>");
		w.println("<head>");
		String lt = getPageLongTitle();
		if( lt.length() > 0 ) w.println("<title>" + XML.xmlEscapeText(lt) + "</title>");
		String cssExternalUri = getExternalResourceUri( req, "style/default.css" );
		w.println("<link rel=\"stylesheet\" type=\"text/css\" href=\""+XML.xmlEscapeAttributeValue(cssExternalUri)+"\"/>");
		w.println("</head><body>");
	}
	
	public void writeFooter(PrintWriter w) {
		w.println("</body></html>");
	}
	
	public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType(getContentType());
		write(response.getWriter());
	}

}
