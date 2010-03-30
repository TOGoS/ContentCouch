package contentcouch.explorify;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import togos.mf.api.Request;
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

	public DirectoryPageGenerator( Directory dir, Request req ) {
		super( req );
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
	
	protected String getPageShortTitle() {
		String name = getArgument("name");
		String pageTitle;
		if( name == null ) {
			pageTitle = getOperandUri();
		} else {
			pageTitle = name + " / " + getOperandUri();
		}
		return pageTitle;
	}
	
	protected String getPageLongTitle() {
		 return getPageShortTitle() + " - ContentCouch directory explorer";
	}
	
	public void writeContent(PrintWriter w) {
		Set entries = dir.getDirectoryEntrySet();
		ArrayList entryList = new ArrayList(entries);
		Collections.sort(entryList, EntryComparators.TYPE_THEN_NAME_COMPARATOR);
		
		w.println("<div class=\"main-content\">");
		w.println("<table class=\"dir-list\">");
		w.write("<tr>");
		w.write("<th>Name</th>");
		w.write("<th>Size</th>");
		w.write("<th>Modified</th>");
		w.write("</tr>\n");
		for( Iterator i=entryList.iterator(); i.hasNext(); ) {
			Entry e = (Entry)i.next();
			String href = getResourceUri(e, true);
			String name = e.getName();
			if( CcouchNamespace.TT_SHORTHAND_DIRECTORY.equals(e.getTargetType()) ) {
				if( !name.endsWith("/") ) name += "/";
			}
			href = getExternalUri("default", getOperandUri(), href, true);
			w.write("<tr>");
			w.write("<td><a href=\"" + XML.xmlEscapeAttributeValue(href) + "\">" + XML.xmlEscapeText(name) + "</a></td>");
			w.write("<td align=\"right\">" + (e.getTargetSize() > -1 ? Long.toString(e.getTargetSize()) : "") + "</td>");
			w.write("<td>" + (e.getTargetLastModified() > -1 ? DateUtil.DISPLAYFORMAT.format(new Date(e.getTargetLastModified())) : "") + "</td>");
			w.write("</tr>\n");
		}
		w.println("</table>");
		w.println("</div>");
	}
}