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
import togos.swf2.HttpServletRequestHandler;
import togos.swf2.SwfFrontRequestHandler;
import togos.swf2.SwfNamespace;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.xml.XML;

public abstract class PageGenerator implements HttpServletRequestHandler {
	protected Request req;
	
	public PageGenerator( Request req ) {
		this.req = req;
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
		SwfFrontRequestHandler f = getSwfFront();
		return (f == null) ? null : f.getExternalComponentUri(req, component, args);
	}
	
	protected String externalUriCache;
	protected String getExternalUri() {
		if( externalUriCache == null ) {
			externalUriCache = getExternalComponentUri(req, getSwfComponent(), getArguments());
		}
		return externalUriCache;
	}
	
	protected String getName() {
		String name = (String)getArguments().getNamedArguments().get("name");
		return (name == null) ? getOperandUri() : name;
	}
	
	////
	
	protected String processUri( String whichProcessor, String uri, String crumbName ) {
		SwfFrontRequestHandler frh = getSwfFront();
		if( frh != null ) {
			if( "raw".equals(whichProcessor) && uri.startsWith(SwfNamespace.SERVLET_PATH_URI_PREFIX)) {
				return frh.getExternalUri(req, uri);
			}
			// eek, whichProcessor currently ignored! :{}
			BaseArguments linkArgs;
			if( crumbName != null ) {
				linkArgs = new BaseArguments(getArguments());
				linkArgs.putNamedArgument("prev", getExternalUri());
				linkArgs.putNamedArgument("name", crumbName);
			} else {
				linkArgs = new BaseArguments();
			}
			linkArgs.putNamedArgument("uri", uri);
			return getExternalComponentUri( req, getSwfComponent(), linkArgs );
		} else {
			return BaseUriProcessor.getInstance( req.getContextVars(), whichProcessor ).processUri(uri);
		}
	}
	
	protected String processUri( String whichProcessor, String uri ) {
		return processUri( whichProcessor, uri, null );
	}
	
	protected String processRelativeUri( String whichProcessor, String baseUri, String relativeUri, String crumbName ) {
		SwfFrontRequestHandler frh = getSwfFront();
		if( frh != null ) {
			return processUri( whichProcessor, PathUtil.appendPath(baseUri, relativeUri), crumbName );
		} else {
			return BaseUriProcessor.getInstance( req.getContextVars(), whichProcessor ).processRelativeUri(baseUri, relativeUri);
		}
	}
	
	protected String processRelativeUri( String whichProcessor, String baseUri, String relativeUri ) {
		return processRelativeUri( whichProcessor, baseUri, relativeUri, null );
	}
	
	protected String generateScriptInclude( String scriptName ) {
		String ppUri = SwfNamespace.SERVLET_PATH_URI_PREFIX + "/lib/js/"+UriUtil.uriEncode(scriptName);
		ppUri = processUri("raw", ppUri);
		return "<script type=\"application/javascript\" src=\"" + XML.xmlEscapeAttributeValue(ppUri) + "\"></script>";
	}
	
	////
	
	public String getContentType() {
		return "text/html; charset=utf-8";
	}
	
	public void writeContent(PrintWriter w) {
		w.println("<p>Nothing to see here.</p>");
	}
	
	public void write(PrintWriter w) {
		writeHeader(w);
		writeContent(w);
		writeFooter(w);
	}
	
	public void writeHeader(PrintWriter w) {
		w.println("<html><body>");
	}
	
	public void writeFooter(PrintWriter w) {
		w.println("</body></html>");
	}
	
	public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType(getContentType());
		write(response.getWriter());
	}
}