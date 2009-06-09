/**
 * 
 */
package contentcouch.explorify;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.eekboom.utils.Strings;

import contentcouch.date.DateUtil;
import contentcouch.misc.Function1;
import contentcouch.misc.ValueUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.rdf.DcNamespace;
import contentcouch.value.Directory;
import contentcouch.value.Ref;
import contentcouch.value.Directory.Entry;
import contentcouch.xml.XML;

public class DirectoryPageGenerator extends PageGenerator {
	final Directory dir;
	final Map metadata;

	public DirectoryPageGenerator( Directory dir, Map metadata, Function1 uriProcessor ) {
		this.dir = dir;
		this.metadata = metadata;
		this.uriProcessor = uriProcessor;
	}
	
	public void write(PrintWriter w) throws IOException {
		Set entries = dir.getDirectoryEntrySet();
		ArrayList entryList = new ArrayList(entries);
		Collections.sort(entryList, new Comparator() {
			public int compare(Object o1, Object o2) {
				Entry e1 = (Entry)o1;
				Entry e2 = (Entry)o2;
				if( e1.getTargetType().equals(e2.getTargetType()) ) {
					return Strings.compareNatural(e1.getKey(),e2.getKey());
				} else {
					if( CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(e1.getTargetType()) ) {
						return -1;
					} else {
						return 1;
					}
				}
			}
		});
		
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
		w.println("<table class=\"dir-list\">");
		w.write("<tr>");
		w.write("<th>Name</th>");
		w.write("<th>Size</th>");
		w.write("<th>Modified</th>");
		w.write("</tr>\n");
		for( Iterator i=entryList.iterator(); i.hasNext(); ) {
			Entry e = (Entry)i.next();
			String href;
			String name = e.getKey();
			if( e.getValue() instanceof Ref ) {
				href = ((Ref)e.getValue()).targetUri;
			} else {
				href = name;
			}
			if( CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(e.getTargetType()) ) {
				if( !PathUtil.isAbsolute(href) && !href.endsWith("/")) href += "/";
				if( !name.endsWith("/") ) name += "/";
			}
			href = processUri(href);
			w.write("<tr>");
			w.write("<td><a href=\"" + XML.xmlEscapeAttributeValue(href) + "\">" + XML.xmlEscapeText(name) + "</a></td>");
			w.write("<td align=\"right\">" + (e.getSize() > -1 ? Long.toString(e.getSize()) : "") + "</td>");
			w.write("<td>" + (e.getLastModified() > -1 ? DateUtil.DISPLAYFORMAT.format(new Date(e.getLastModified())) : "") + "</td>");
			w.write("</tr>\n");
		}
		w.println("</table>");
		w.println("</body>");
		w.println("</html>");
	}
}