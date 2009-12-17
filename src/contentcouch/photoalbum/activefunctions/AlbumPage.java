package contentcouch.photoalbum.activefunctions;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.swf2.SwfNamespace;
import contentcouch.activefunctions.Explorify;
import contentcouch.builtindata.BuiltInData;
import contentcouch.date.DateUtil;
import contentcouch.directory.EntryComparators;
import contentcouch.directory.HasLongPath;
import contentcouch.explorify.BaseUriProcessor;
import contentcouch.explorify.DirectoryPageGenerator;
import contentcouch.explorify.PageGenerator;
import contentcouch.explorify.UriProcessor;
import contentcouch.json.JSON;
import contentcouch.misc.UriUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;
import contentcouch.value.Directory.Entry;
import contentcouch.xml.XML;

public class AlbumPage extends Explorify {
	protected static String getThumbnailUri( String imageUri, Map context ) {
		return
			"active:contentcouch.graphics.thumbnail+operand@" + UriUtil.uriEncode(imageUri) +
			"+width@data:,128+height@data:,128";
	}
	
	protected static String getPreviewUri( String imageUri, Map context ) {
		return
			"active:contentcouch.graphics.thumbnail+operand@" + UriUtil.uriEncode(imageUri) +
			"+width@data:,640+height@data:,480";

	}
	
	protected static class AlbumPageGenerator extends DirectoryPageGenerator {
		public AlbumPageGenerator( Directory dir, String uri, Map context, String header, String footer ) {
			super( dir, uri, context, header, footer );
		}
		
		public void write(PrintWriter w) {
			UriProcessor rawUriProcessor = BaseUriProcessor.getInstance( context, "raw" );
			
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

			////
			
			w.println("<html>");
			w.println("<head>");
			w.println("<style>/*<![CDATA[*/");
			w.println(BuiltInData.getString("default-page-style"));
			w.println("/*]]>*/</style>");
			if( imageEntryList.size() > 0 ) {
				w.println( generateScriptInclude("module.js") );
				w.println( generateScriptInclude("contentcouch/photoalbum/PhotoPreviewer.js") );
				w.println("<script type=\"application/javascript\">//<![CDATA[");
				w.println("var pp = new contentcouch.photoalbum.PhotoPreviewer();");
				for( Iterator i=imageEntryList.iterator(); i.hasNext(); ) {
					Entry e = (Entry)i.next();
					String imageUri = getUnprocessedHref(e, false);
					String shrunkUri = getThumbnailUri(imageUri, context);
					String previewUri = getPreviewUri(imageUri, context);
					w.println("pp.addPreview("+
						JSON.encodeObject(rawUriProcessor.processUri(shrunkUri))+","+
						JSON.encodeObject(rawUriProcessor.processUri(previewUri))+","+
						JSON.encodeObject(rawUriProcessor.processUri(imageUri))+
					");");
				}
				w.println("function goToPreview(index) { return pp.goToPreview(index); }");
				w.println("function goToPreviousPreview() { return pp.goToPreviousPreview(); }");
				w.println("function goToNextPreview() { return pp.goToNextPreview(); }");
				w.println("function getPreviewer() { return pp; }");
				w.println("//]]></script>");
			}
			w.println("</head>");
			w.println("<body>");
			w.println("<h2>Viewing "+context.get("processed-uri")+"</h2>");

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
			
			if( imageEntryList.size() > 0 ) {
				w.println("<h3>Images</h3>");
				
				w.println("<div id=\"preview-container\" class=\"preview-container\" style=\"display:none;\">");

				w.println("<div class=\"preview-close-box\"><a onclick=\"return goToPreview(null)\" href=\"#\">Close preview window</a></div>");

				w.println("<div style=\"float:left; width: 680px; height:540px\" class=\"preview-inner-box\">");
				w.println("<a id=\"preview-link\" href=\"\"><img id=\"preview-image\"/></a><br />");
				w.println("</div>");
				
				//w.println("<div style=\"margin:0; padding:0; float:right; clear:right\"></div>");
				
				w.println("<div id=\"previous-link-box\" class=\"preview-nav-box\">");
				w.println("<div class=\"image-thumbnail-title\">Previous</div>");
				w.print("<div class=\"image-thumbnail-inner-box\">");
				w.print("<a onclick=\"return goToPreviousPreview()\" href=\"#\">");
				w.print("<img id=\"previous-thumbnail\" src=\"\"/>");
				w.println("</a></div></div>");

				w.println("<div id=\"next-link-box\" class=\"preview-nav-box\">");
				w.println("<div class=\"image-thumbnail-title\">Next</div>");
				w.print("<div class=\"image-thumbnail-inner-box\">");
				w.print("<a onclick=\"return goToNextPreview()\" href=\"#\">");
				w.print("<img id=\"next-thumbnail\" src=\"\"/>");
				w.println("</a></div></div>");

				/*
				w.println("<div class=\"preview-nav\" id=\"previous-link-box\"><a onclick=\"return goToPreviousPreview()\" href=\"#\">Previous<div class=\"image-thumbnail-inner-box\"><img id=\"previous-thumbnail\"/></div></a></div>");
				w.println("<div class=\"preview-nav\" id=\"next-link-box\"><a onclick=\"return goToNextPreview()\" href=\"#\">Next<div class=\"image-thumbnail-inner-box\"><img id=\"next-thumbnail\"/></div></a></div>");
				*/
				w.println("</div>");

				int index = 0;
				for( Iterator i=imageEntryList.iterator(); i.hasNext(); ) {
					Entry e = (Entry)i.next();
					String imageUri = getUnprocessedHref(e, false);
					/*
					String shrunkUri =
						"active:contentcouch.graphics.serialize-image+operand@" + UriUtil.uriEncode( 
							"active:contentcouch.graphics.scale-image+operand@" + UriUtil.uriEncode(imageUri) +
							"+max-width@" + UriUtil.uriEncode(UriUtil.makeDataUri("128")) +
							"+max-height@" + UriUtil.uriEncode(UriUtil.makeDataUri("128"))
						) + "+format@" + UriUtil.uriEncode(UriUtil.makeDataUri("jpeg"));
					*/
					String shrunkUri =
						"active:contentcouch.graphics.thumbnail+operand@" + UriUtil.uriEncode(imageUri) +
						"+width@data:,128+height@data:,128";
					w.println("<div class=\"image-thumbnail-box\">");
					w.println("<div class=\"image-thumbnail-title\">" + e.getName() + "</div>");
					w.print("<div class=\"image-thumbnail-inner-box\">");
					w.print("<a onclick=\"return goToPreview("+index+")\" href=\"" + XML.xmlEscapeAttributeValue(processUri("explore",imageUri)) + "\">");
					w.print("<img src=\"" + XML.xmlEscapeAttributeValue(rawUriProcessor.processUri(shrunkUri)) + "\"/>");
					w.println("</a></div></div>");
					++index;
				}
				
				String loadingImageUri = SwfNamespace.SERVLET_PATH_URI_PREFIX + "style/ajax-loader.gif";
				
				w.println("<script type=\"application/javascript\">//<![CDATA[");
				w.println("pp.loadingImageUrl   = "+JSON.encodeObject(rawUriProcessor.processUri(loadingImageUri))+";");
				w.println("pp.previewContainer  = document.getElementById('preview-container');");
				w.println("pp.previewLink       = document.getElementById('preview-link');");
				w.println("pp.previewImage      = document.getElementById('preview-image');");
				w.println("pp.nextLinkBox       = document.getElementById('next-link-box');");
				w.println("pp.nextThumbnail     = document.getElementById('next-thumbnail');");
				w.println("pp.previousLinkBox   = document.getElementById('previous-link-box');");
				w.println("pp.previousThumbnail = document.getElementById('previous-thumbnail');");
				w.println("window.onkeydown = function(evt) { switch(evt.keyCode) { case(39): goToNextPreview(); break; case(37): goToPreviousPreview(); } }");
				w.println("</script>");
			}
			w.println("<div style=\"clear:both\"></div>");
			w.println("</div>");

			/*
			w.println("<script type=\"application/javascript\">//<![CDATA[");
			w.println("getPreviewer().showPreviewBasedOnUrl();");
			w.println("</script>");
			*/
			
			w.println("</body>");
			w.println("</html>");
		}
	}
	
	protected static class AlbumEntryPageGenerator extends PageGenerator {
		Directory.Entry directoryEntry;
		
		public AlbumEntryPageGenerator( Directory.Entry de, String uri, Map context, String header, String footer ) {
			super( uri, context, header, footer );
			directoryEntry = de;
		}
		
		public void writeContent(PrintWriter w) {
			UriProcessor rawUriProcessor = BaseUriProcessor.getInstance( context, "raw" );

			String imageUri = TheGetter.reference(directoryEntry.getTarget(), true, true);
			String previewUri = getPreviewUri(imageUri, context);
			
			w.println("<div class=\"preview-container\" style=\"text-align:center\">");
			w.println("<div style=\"width: 680px; height:540px; margin:auto\" class=\"preview-inner-box\">");
			w.print("<a href=\"" + XML.xmlEscapeAttributeValue(rawUriProcessor.processUri(imageUri)) + "\">");
			w.print("<img src=\"" + XML.xmlEscapeAttributeValue(rawUriProcessor.processUri(previewUri)) + "\"/>");
			w.print("</a>");
			w.print("<div class=\"preview-caption\">");
			
			String caption;
			if( directoryEntry instanceof HasLongPath ) {
				caption = ((HasLongPath)directoryEntry).getLongPath();
			} else {
				caption = directoryEntry.getName();
			}
			if( caption.length() > 75 ) {
				caption = "..."+caption.substring(caption.length()-73);
			}
			
			w.print(XML.xmlEscapeText(caption));
			w.println("</div>");
			w.println("</div>");
			w.println("</div>");
		}
	}
	
	public Response explorifyDirectory( Request req, String uri, Directory d, String header, String footer ) {
		return getPageGeneratorResult(new AlbumPageGenerator(d, uri, req.getContextVars(), header, footer ));
	}
	
	public Response explorifyDirectoryEntry(Request req, Map argumentExpressions, String uri, Directory.Entry de ) {
		return getPageGeneratorResult(new AlbumEntryPageGenerator(de, uri, req.getContextVars(), getHeader(req, argumentExpressions), getFooter(req, argumentExpressions) ));
	}

	/*
	public Response call(Map argumentExpressions) {
		Expression e = (Expression)argumentExpressions.get("operand");
		String uri = e.toUri();
		Context.pushNewDynamicScope();
		try {
			Object v = getArgumentValue(argumentExpressions, "operand", null);
			Context.put("processed-uri", uri);
			if( v instanceof Directory ) {
				Directory dir = (Directory)v;
				String header = getHeader(argumentExpressions);
				String footer = getFooter(argumentExpressions);
				return getPageGeneratorResult(new AlbumPageGenerator(dir, uri, Context.getInstance(), header, footer ));
			} else {
				
			}
		} finally {
			Context.popInstance();
		}
	}
	*/

	protected String getPathArgumentName() {
		return "operand";
	}
}
