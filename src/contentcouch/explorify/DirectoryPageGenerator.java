package contentcouch.explorify;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.base.BaseArguments;
import contentcouch.date.DateUtil;
import contentcouch.directory.EntryComparators;
import contentcouch.misc.UriUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Directory;
import contentcouch.value.Ref;
import contentcouch.value.RelativeRef;
import contentcouch.value.Directory.Entry;
import contentcouch.xml.XML;

public class DirectoryPageGenerator extends CCouchExplorerPageGenerator {
	protected Directory dir;

	public DirectoryPageGenerator( Request req, Response resourceResponse, Directory dir ) {
		super( req, resourceResponse );
		this.dir = dir;
	}
	
	protected boolean isDirectory( Directory.Entry e ) {
		return CcouchNamespace.TT_SHORTHAND_DIRECTORY.equals(e.getTargetType());
	}
	
	/**
	 * Returns the URI of the targeted resource.
	 * If allowRelative & allowRelativeResourceUris is true, this may be a relative URI
	 * If preferRelative & allowRelativeResourceUris is true, it will almost always be a relative URI
	 * @param e
	 * @return
	 */
	protected String getResourceUri( Directory.Entry e, boolean allowRelative, boolean preferRelative ) {
		String href;
		if( preferRelative & allowRelative && allowRelativeResourceUris ) {
			href = allowRelativeResourceUris ? UriUtil.uriEncode(e.getName()) : PathUtil.appendPath(getOperandUri(), UriUtil.uriEncode(e.getName()));
		} else if( e.getTarget() instanceof RelativeRef && ((RelativeRef)e.getTarget()).isRelative() ) {
			href = ((RelativeRef)e.getTarget()).getTargetRelativeUri();
		} else if( e.getTarget() instanceof Ref ) {
			href = ((Ref)e.getTarget()).getTargetUri();
		} else {
			href = (allowRelative & allowRelativeResourceUris) ? UriUtil.uriEncode(e.getName()) : PathUtil.appendPath(getOperandUri(), UriUtil.uriEncode(e.getName()));
		}
		if( isDirectory(e) ) {
			if( !PathUtil.isAbsolute(href) && !href.endsWith("/")) href += "/";	
		}
		return href;
	}
	
	protected String getResourceUri( Directory.Entry e, boolean allowRelative ) {
		return getResourceUri( e, allowRelative, preferRelativeResourceUris );
	}

	protected String getResourceUri( Directory.Entry e ) {
		return getResourceUri( e, false, false );
	}
	
	protected String getExternalUri( String componentName, Directory.Entry e, boolean allowRelative ) {
		String internalUri = getResourceUri( e, allowRelative );
		if( PathUtil.isAbsolute(internalUri) ) {
			String tt = e.getTargetType();
			BaseArguments args = new BaseArguments();
			args.putNamedArgument("uri", internalUri);
			args.putNamedArgument("name", e.getName());
			args.putNamedArgument("objectType", tt);
			return getExternalComponentUri( req, componentName, args );
		} else {
			return getExternalUri( componentName, internalUri, allowRelative );
		}
	}
	
	protected String getExternalUri( Directory.Entry e, boolean allowRelative ) {
		return getExternalUri( null, e, allowRelative );
	}
	
	protected String getExternalUri( Directory.Entry e ) {
		boolean allowRelative = isDirectory(e) || !alwaysRebaseBlobUris;
		return getExternalUri( null, e, allowRelative );
	}

	protected String getOperandResolvedUrn() {
		String resolvedUri = (String)resourceResponse.getMetadata().get(CcouchNamespace.RES_RESOLVED_URI);
		return resolvedUri;
	}

	protected String getPath() {
		String path = (String)getArguments().getNamedArguments().get("path");
		return (path == null) ? getName() : path;
	}
	
	protected String getPageShortTitle() {
		return getPath();
	}
	
	protected String getPageLongTitle() {
		 return getPageShortTitle() + " - ContentCouch directory explorer";
	}
	
	protected void writeDirectLink(PrintWriter w) {
		String resolvedUrn = getOperandResolvedUrn();
		if( resolvedUrn != null && !resolvedUrn.equals(getOperandUri()) ) {
			String href = getExternalUriWithName(null, resolvedUrn, getShortName(), CcouchNamespace.DIRECTORY);
			w.println("<ul class=\"crumbtrail\"><li><a href=\""+XML.xmlEscapeAttributeValue(href)+"\" title=\"Short URL for this page\">"+XML.xmlEscapeText(resolvedUrn)+"</a></li></ul>");
		}
	}
	
	public void writeContent(PrintWriter w) {
		Set entries = dir.getDirectoryEntrySet();
		ArrayList entryList = new ArrayList(entries);
		Collections.sort(entryList, EntryComparators.TYPE_THEN_NAME_COMPARATOR);
		
		writeDirectLink(w);
		
		w.println("<div class=\"main-content\">");
		w.println("<table class=\"dir-list\">");
		w.write("<tr>");
		w.write("<th>Name</th>");
		w.write("<th>Size</th>");
		w.write("<th>Modified</th>");
		w.write("</tr>\n");
		for( Iterator i=entryList.iterator(); i.hasNext(); ) {
			Entry e = (Entry)i.next();
			String name = e.getName();
			if( isDirectory(e) ) name += "/";
			String href = getExternalUri(e);
			w.write("<tr>");
			w.write("<td><a href=\"" + XML.xmlEscapeAttributeValue(href) + "\">" + XML.xmlEscapeText(name) + "</a></td>");
			w.write("<td align=\"right\">" + (e.getTargetSize() > -1 ? Long.toString(e.getTargetSize()) : "") + "</td>");
			w.write("<td>" + (e.getLastModified() > -1 ? DateUtil.DISPLAYFORMAT.format(new Date(e.getLastModified())) : "") + "</td>");
			w.write("</tr>\n");
		}
		w.println("</table>");
		w.println("</div>");
	}
}