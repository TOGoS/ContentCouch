package contentcouch.explorify;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import togos.mf.api.Request;
import togos.mf.api.Response;
import contentcouch.date.DateUtil;
import contentcouch.directory.EntryComparators;
import contentcouch.directory.HasLongPath;
import contentcouch.framework.TheGetter;
import contentcouch.json.JSON;
import contentcouch.misc.UriUtil;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.value.Directory;
import contentcouch.value.Directory.Entry;
import contentcouch.xml.XML;

public class AlbumPage {
	protected static String getThumbnailUri( String imageUri, int width, int height ) {
		return
			"active:contentcouch.graphics.thumbnail+operand@" + UriUtil.uriEncode(imageUri) +
			"+width@data:,"+width+"+height@data:,"+height;
	}
	
	protected static String getThumbnailName( String name, int w, int h ) {
		int li = name.lastIndexOf('.');
		if( li == -1 ) {
			return name + "-"+w+"x"+h;
		} else {
			return name.substring(0,li)+"-"+w+"x"+h+name.substring(li);
		}
	}
	
	public static class AlbumPageGenerator extends DirectoryPageGenerator {
		public AlbumPageGenerator( Request req, Response subRes, Directory dir ) {
			super( req, subRes, dir );
		}
		
		protected String getPageLongTitle() {
			 return getPageShortTitle() + " - ContentCouch album explorer";
		}
		
		public void write(PrintWriter w) {
			Set entries = dir.getDirectoryEntrySet();

			ArrayList dirEntryList = new ArrayList();
			ArrayList imageEntryList = new ArrayList();
			ArrayList miscEntryList = new ArrayList();
			
			for( Iterator i=entries.iterator(); i.hasNext(); ) {
				Directory.Entry e = (Directory.Entry)i.next();
				
				if( CCouchNamespace.TT_SHORTHAND_DIRECTORY.equals(e.getTargetType()) ) {
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
			String cssExternalUri = getExternalResourceUri( req, "style/default.css" );
			w.println("<link rel=\"stylesheet\" type=\"text/css\" href=\""+XML.xmlEscapeAttributeValue(cssExternalUri)+"\"/>");
			if( imageEntryList.size() > 0 ) {
				w.println( generateScriptInclude("module.js") );
				w.println( generateScriptInclude("contentcouch/photoalbum/PhotoPreviewer.js") );
				w.println("<script type=\"application/javascript\">//<![CDATA[");
				w.println("var pp = new contentcouch.photoalbum.PhotoPreviewer();");
				for( Iterator i=imageEntryList.iterator(); i.hasNext(); ) {
					Entry e = (Entry)i.next();
					String imageUri = getResourceUri(e); 
					String imageHref = getExternalUri("raw", e, false);
					String shrunkUri = getThumbnailUri(imageUri, 128, 128);
					String shrunkName = getThumbnailName(e.getName(), 128, 128);
					String previewUri = getThumbnailUri(imageUri, 640, 480);
					String previewName = getThumbnailName(e.getName(), 640, 480);
					w.println("pp.addPreview("+
						JSON.encodeObject(getExternalBlobUri(shrunkUri, shrunkName))+","+
						JSON.encodeObject(getExternalBlobUri(previewUri, previewName))+","+
						JSON.encodeObject(imageHref)+
					");");
				}
				w.println("function goToPreview(index) { return pp.goToPreview(index); }");
				w.println("function goToPreviousPreview() { return pp.goToPreviousPreview(); }");
				w.println("function goToNextPreview() { return pp.goToNextPreview(); }");
				w.println("function getPreviewer() { return pp; }");
				w.println("//]]></script>");
			}
			w.println("<title>" + XML.xmlEscapeText(getPageLongTitle() + " - album view") + "</title>");
			w.println("</head>");
			w.println("<body>");
			
			w.println("<h2>" + XML.xmlEscapeText(getPageShortTitle()) + "</h2>");
			
			writeDirectLink(w);
			
			w.println("<div class=\"main-content\">");
			if( dirEntryList.size() > 0 ) {
				w.println("<h3 title=\"Short URL\">Subdirectories</h3>");
				w.println("<table class=\"dir-list\">");
				w.write("<tr>");
				w.write("<th colspan=\"2\">Name</th>");
				w.write("<th title=\"Short URL\">Size</th>");
				w.write("<th>Modified</th>");
				w.write("</tr>\n");
				for( Iterator i=dirEntryList.iterator(); i.hasNext(); ) {
					Entry e = (Entry)i.next();
					String href = getExternalUri(e);
					String directHref = getExternalUri(e,false);
					String ename = e.getName() + (isDirectory(e) ? "/" : "");
					w.write("<tr>");
					w.write("<td><a href=\"" + XML.xmlEscapeAttributeValue(href) + "\">" + XML.xmlEscapeText(ename) + "</a></td>");
					w.write("<td class=\"colext\">");
					if( !directHref.equals(href) ) {
						w.write(" <a class=\"tinylink\" href=\"" + XML.xmlEscapeAttributeValue(directHref) + "\" title=\"Short URL\">S</a>");
					}
					w.write("</td>");
					w.write("<td align=\"right\">" + (e.getTargetSize() > -1 ? Long.toString(e.getTargetSize()) : "") + "</td>");
					w.write("<td>" + (e.getLastModified() > -1 ? DateUtil.DISPLAYFORMAT.format(new Date(e.getLastModified())) : "") + "</td>");
					w.write("</tr>\n");
				}
				w.println("</table>");
			}
			
			if( miscEntryList.size() > 0 ) {
				w.println("<h3>Misc. files</h3>");
				w.println("<table class=\"dir-list\">");
				w.write("<tr>");
				w.write("<th colspan=\"2\">Name</th>");
				w.write("<th title=\"Short URL\">Size</th>");
				w.write("<th>Modified</th>");
				w.write("</tr>\n");
				for( Iterator i=miscEntryList.iterator(); i.hasNext(); ) {
					Entry e = (Entry)i.next();
					String href = getExternalUri(e);
					String directHref = getExternalUri(e,false);
					String ename = e.getName() + (isDirectory(e) ? "/" : "");
					w.write("<tr>");
					w.write("<td><a href=\"" + XML.xmlEscapeAttributeValue(href) + "\">" + XML.xmlEscapeText(ename) + "</a></td>");
					w.write("<td class=\"colext\">");
					if( !directHref.equals(href) ) {
						w.write(" <a class=\"tinylink\" href=\"" + XML.xmlEscapeAttributeValue(directHref) + "\" title=\"Short URL\">S</a>");
					}
					w.write("</td>");
					w.write("<td align=\"right\">" + (e.getTargetSize() > -1 ? Long.toString(e.getTargetSize()) : "") + "</td>");
					w.write("<td>" + (e.getLastModified() > -1 ? DateUtil.DISPLAYFORMAT.format(new Date(e.getLastModified())) : "") + "</td>");
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

				w.println("</div>");

				int index = 0;
				for( Iterator i=imageEntryList.iterator(); i.hasNext(); ) {
					Entry e = (Entry)i.next();
					String imageHref = getExternalUri(e);
					String directHref = getExternalUri("raw",e,false);
					String absoluteImageUri = getResourceUri(e);
					String shrunkUri = getThumbnailUri(absoluteImageUri, 128, 128);
					String shrunkName = getThumbnailName(e.getName(), 128, 128);
					w.println("<div class=\"image-thumbnail-box\">");
					w.println("<div class=\"image-thumbnail-title\">" + e.getName() + "</div>");
					w.print("<div class=\"image-thumbnail-inner-box\">");
					
					w.print("<a onclick=\"return goToPreview("+index+")\" href=\"" + XML.xmlEscapeAttributeValue(imageHref) + "\">");
					w.print("<img src=\"" + XML.xmlEscapeAttributeValue(getExternalBlobUri(shrunkUri, shrunkName)) + "\"/>");
					w.print("</a>");
					w.println("</div>");
					w.println("<div class=\"image-thumbnail-footer\">");
					w.println("<a class=\"tinylink\" href=\"" + XML.xmlEscapeAttributeValue(directHref) + "\" title=\"Short URL\">S</a>");
					w.print("</div>");
					w.println("</div>");
					++index;
				}
				
				String loadingImageExternalUri = getExternalResourceUri( req, "style/ajax-loader.gif" );
				
				w.println("<script type=\"application/javascript\">//<![CDATA[");
				w.println("pp.loadingImageUrl   = "+JSON.encodeObject( loadingImageExternalUri )+";");
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
	
	protected static class AlbumEntryPageGenerator extends CCouchExplorerPageGenerator {
		Directory.Entry directoryEntry;
		
		public AlbumEntryPageGenerator( Request req, Response subRes, Directory.Entry de ) {
			super( req, subRes );
			directoryEntry = de;
		}
		
		public void writeContent(PrintWriter w) {
			String imageUri = TheGetter.reference(directoryEntry.getTarget(), true, true);
			String imageName = directoryEntry.getName(); 
			String previewUri = getThumbnailUri(imageUri, 640, 480);
			String previewName = getThumbnailName(imageName, 640, 480);
			
			w.println("<div class=\"preview-container\" style=\"text-align:center\">");
			w.println("<div style=\"width: 680px; height:540px; margin:auto\" class=\"preview-inner-box\">");
			w.print("<a href=\"" + XML.xmlEscapeAttributeValue(getExternalBlobUri(imageUri,imageName)) + "\">");
			w.print("<img src=\"" + XML.xmlEscapeAttributeValue(getExternalBlobUri(previewUri,previewName)) + "\"/>");
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
}
