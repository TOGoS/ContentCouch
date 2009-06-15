package contentcouch.explorify;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import contentcouch.date.DateUtil;
import contentcouch.directory.EntryComparators;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Directory;
import contentcouch.value.Ref;
import contentcouch.value.RelativeRef;
import contentcouch.value.Directory.Entry;
import contentcouch.xml.XML;

public class DirectoryPageGenerator extends PageGenerator {
	Directory dir;

	public DirectoryPageGenerator( Directory dir, String uri, UriProcessor uriProcessor, String header, String footer ) {
		super( uri, uriProcessor, header, footer );
		this.dir = dir;
	}
	
	public void writeContent(PrintWriter w) {
		Set entries = dir.getDirectoryEntrySet();
		ArrayList entryList = new ArrayList(entries);
		Collections.sort(entryList, EntryComparators.TYPE_THEN_NAME_COMPARATOR);
		
		/*
		String title = ValueUtil.getString(metadata.get(DcNamespace.DC_TITLE));
		if( title == null ) title = "Index of some directory";

		w.println("<html>");
		w.println("<head>");
		w.println("<title>" + XML.xmlEscapeText(title) + "</title>");
		w.println("<style>");
		w.println(".dir-list td, .dir-list th { padding-left: 1ex; padding-right: 1ex; font-family: Courier }");
		w.println("</style>");
		w.println("</head>");
		w.println("<body>");
		w.println("<h2>" + XML.xmlEscapeText(title) + "</h2>");
		*/
		
		w.println("<div class=\"main-content\">");
		w.println("<table class=\"dir-list\">");
		w.write("<tr>");
		w.write("<th>Name</th>");
		w.write("<th>Size</th>");
		w.write("<th>Modified</th>");
		w.write("</tr>\n");
		for( Iterator i=entryList.iterator(); i.hasNext(); ) {
			Entry e = (Entry)i.next();
			String href;
			String name = e.getName();
			if( e.getTarget() instanceof RelativeRef && ((RelativeRef)e.getTarget()).isRelative() ) {
				href = ((RelativeRef)e.getTarget()).getTargetRelativeUri();
			} else if( e.getTarget() instanceof Ref ) {
				href = ((Ref)e.getTarget()).getTargetUri();
			} else {
				href = name;
			}
			if( CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(e.getTargetType()) ) {
				if( !PathUtil.isAbsolute(href) && !href.endsWith("/")) href += "/";
				if( !name.endsWith("/") ) name += "/";
			}
			href = processRelativeUri(uri, href);
			w.write("<tr>");
			w.write("<td><a href=\"" + XML.xmlEscapeAttributeValue(href) + "\">" + XML.xmlEscapeText(name) + "</a></td>");
			w.write("<td align=\"right\">" + (e.getTargetSize() > -1 ? Long.toString(e.getTargetSize()) : "") + "</td>");
			w.write("<td>" + (e.getTargetLastModified() > -1 ? DateUtil.DISPLAYFORMAT.format(new Date(e.getTargetLastModified())) : "") + "</td>");
			w.write("</tr>\n");
		}
		w.println("</table>");
		w.println("</div>");
		
		/*
		w.println("</body>");
		w.println("</html>");
		*/
	}
}