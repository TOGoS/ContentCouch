package contentcouch.photoalbum.activefunctions;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import togos.mf.api.Response;
import contentcouch.active.Context;
import contentcouch.active.expression.Expression;
import contentcouch.activefunctions.Explorify;
import contentcouch.date.DateUtil;
import contentcouch.directory.EntryComparators;
import contentcouch.explorify.DirectoryPageGenerator;
import contentcouch.misc.UriUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Directory;
import contentcouch.value.Directory.Entry;
import contentcouch.xml.XML;

public class MakeAlbumPage extends Explorify {
	protected class AlbumPageGenerator extends DirectoryPageGenerator {
		public AlbumPageGenerator( Directory dir, String uri, Map context, String header, String footer ) {
			super( dir, uri, context, header, footer );
		}
		
		public void writeContent(PrintWriter w) {
			Set entries = dir.getDirectoryEntrySet();

			ArrayList dirEntryList = new ArrayList();
			ArrayList imageEntryList = new ArrayList();
			ArrayList miscEntryList = new ArrayList();
			
			for( Iterator i=entries.iterator(); i.hasNext(); ) {
				Directory.Entry e = (Directory.Entry)i.next();
				
				if( CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(e.getTargetType()) ) {
					dirEntryList.add(e);
					continue;
				}

				String n = e.getName().toLowerCase();
				if( n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif") ) {
					imageEntryList.add(e);
					continue;
				}

				miscEntryList.add(e);
			}
			
			Collections.sort(dirEntryList, EntryComparators.NAME_COMPARATOR);
			Collections.sort(imageEntryList, EntryComparators.NAME_COMPARATOR);
			Collections.sort(miscEntryList, EntryComparators.NAME_COMPARATOR);

			w.println("<div class=\"main-content\">");
			if( dirEntryList.size() > 0 ) {
				w.println("<h3>Subdirectories</h3>");
				w.println("<table class=\"dir-list\">");
				w.write("<tr>");
				w.write("<th>Name</th>");
				w.write("<th>Size</th>");
				w.write("<th>Modified</th>");
				w.write("</tr>\n");
				for( Iterator i=dirEntryList.iterator(); i.hasNext(); ) {
					Entry e = (Entry)i.next();
					String href = getUnprocessedHref( e, true );
					href = processRelativeUri("album", uri, href);
					String name = e.getName();
					if( CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(e.getTargetType()) ) {
						if( !name.endsWith("/") ) name += "/";
					}
					w.write("<tr>");
					w.write("<td><a href=\"" + XML.xmlEscapeAttributeValue(href) + "\">" + XML.xmlEscapeText(name) + "</a></td>");
					w.write("<td align=\"right\">" + (e.getTargetSize() > -1 ? Long.toString(e.getTargetSize()) : "") + "</td>");
					w.write("<td>" + (e.getTargetLastModified() > -1 ? DateUtil.DISPLAYFORMAT.format(new Date(e.getTargetLastModified())) : "") + "</td>");
					w.write("</tr>\n");
				}
				w.println("</table>");
			}
			if( miscEntryList.size() > 0 ) {
				w.println("<h3>Misc. files</h3>");
				w.println("<table class=\"dir-list\">");
				w.write("<tr>");
				w.write("<th>Name</th>");
				w.write("<th>Size</th>");
				w.write("<th>Modified</th>");
				w.write("</tr>\n");
				for( Iterator i=miscEntryList.iterator(); i.hasNext(); ) {
					Entry e = (Entry)i.next();
					String href = getUnprocessedHref( e, false );
					href = processRelativeUri("raw", uri, href);
					String name = e.getName();
					if( CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(e.getTargetType()) ) {
						if( !name.endsWith("/") ) name += "/";
					}
					w.write("<tr>");
					w.write("<td><a href=\"" + XML.xmlEscapeAttributeValue(href) + "\">" + XML.xmlEscapeText(name) + "</a></td>");
					w.write("<td align=\"right\">" + (e.getTargetSize() > -1 ? Long.toString(e.getTargetSize()) : "") + "</td>");
					w.write("<td>" + (e.getTargetLastModified() > -1 ? DateUtil.DISPLAYFORMAT.format(new Date(e.getTargetLastModified())) : "") + "</td>");
					w.write("</tr>\n");
				}
				w.println("</table>");
			}
			
			if( imageEntryList.size() > 0 ) {
				w.println("<h3>Images</h3>");
				for( Iterator i=imageEntryList.iterator(); i.hasNext(); ) {
					Entry e = (Entry)i.next();
					String imageUri = getUnprocessedHref(e, false);
					String shrunkUri =
						"active:contentcouch.graphics.serialize-image+operand@" + UriUtil.uriEncode( 
							"active:contentcouch.graphics.scale-image+operand@" + UriUtil.uriEncode(imageUri) +
							"+max-width@" + UriUtil.uriEncode(UriUtil.makeDataUri("128")) +
							"+max-height@" + UriUtil.uriEncode(UriUtil.makeDataUri("128"))
						) + "+format@" + UriUtil.uriEncode(UriUtil.makeDataUri("jpeg"));
					w.println("<div class=\"image-thumbnail-box\">");
					w.println("<div class=\"image-thumbnail-title\">" + e.getName() + "</div>");
					w.print("<div class=\"image-thumbnail-inner-box\">");
					w.print("<a href=\"" + XML.xmlEscapeAttributeValue(processUri("explore",imageUri)) + "\">");
					w.print("<img src=\"" + XML.xmlEscapeAttributeValue(processUri("explore",shrunkUri)) + "\"/>");
					w.println("</a></div></div>");
				}
			}
			w.print("<div style=\"clear:both\"></div>");
			w.println("</div>");
		}
	}
	
	public Response call(Map argumentExpressions) {
		Expression e = (Expression)argumentExpressions.get("operand");
		String uri = e.toUri();
		Context.pushNewDynamicScope();
		try {
			Context.put("processed-uri", uri);
			Directory dir = (Directory)getArgumentValue(argumentExpressions, "operand", null);
			String header = getHeader(argumentExpressions);
			String footer = getFooter(argumentExpressions);
			return getPageGeneratorResult(new AlbumPageGenerator(dir, uri, Context.getInstance(), header, footer ));
		} finally {
			Context.popInstance();
		}
	}

	protected String getPathArgumentName() {
		return "operand";
	}
}
