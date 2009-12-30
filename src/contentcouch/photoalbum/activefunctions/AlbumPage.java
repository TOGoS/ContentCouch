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
import togos.mf.base.BaseArguments;
import togos.mf.value.Arguments;
import togos.swf2.Component;
import togos.swf2.SwfFrontRequestHandler;
import togos.swf2.SwfNamespace;
import contentcouch.activefunctions.Explorify;
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
	
	public static class AlbumPageGenerator extends DirectoryPageGenerator {
		static class Crumb {
			public String uri;
			public String name;
			public Crumb next;
			public Crumb( String uri, String name, Crumb next ) {
				this.uri = uri;
				this.name = name;
				this.next = next;
			}
		}
		
		/**
		 * Given the URI of the latest node in a crumb trail, returns the
		 * earliest node 
		 * @param uri URI of the current node
		 * @param maxDepth maximum number of items to return
		 * @param next the next node
		 * @return
		 */
		static Crumb parseCrumbTrail( String uri, int maxDepth, Crumb next ) {
			if( maxDepth == 0 ) return next;
			if( uri == null ) return next;
			Arguments args = parseArgumentsFromUri( uri );
			String prev = (String)args.getNamedArguments().get("prev");
			String name = (String)args.getNamedArguments().get("name");
			String opUri = (String)args.getNamedArguments().get("uri");
			if( name == null ) name = opUri;
			if( name == null ) name = "(no name or opUri??)";
			if( prev == null ) {
				return new Crumb( uri, name, next );
			} else {
				return parseCrumbTrail( prev, maxDepth-1, new Crumb( uri, name, next ) );
			}
		}

		
		public AlbumPageGenerator( Directory dir, Request req ) {
			super( dir, req );
		}
		
		protected String getRawUri( String operandUri ) {
			SwfFrontRequestHandler swf = getSwfFront();
			if( swf == null ) {
				return processUri("raw", operandUri);
			} else {
				// TODO: add raw component and replace album with raw!
				Component raw = (Component)((Map)req.getContextVars().get(SwfNamespace.COMPONENTS)).get("album");
				BaseArguments linkArgs = new BaseArguments();
				linkArgs.putNamedArgument("uri", operandUri);
				return getExternalComponentUri( req, raw, linkArgs );
			}
		}
		
		public void write(PrintWriter w) {
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
			
			String name = getArgument("name");
			String pageTitle;
			if( name == null ) {
				pageTitle = getOperandUri();
			} else {
				pageTitle = name + " / " + getOperandUri();
			}
			
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
					String imageUri = getUnprocessedHref(e, false);
					String shrunkUri = getThumbnailUri(imageUri, req.getContextVars());
					String previewUri = getPreviewUri(imageUri, req.getContextVars());
					w.println("pp.addPreview("+
						JSON.encodeObject(processUri("raw", shrunkUri))+","+
						JSON.encodeObject(processUri("raw", previewUri))+","+
						JSON.encodeObject(processUri("raw", imageUri))+
					");");
				}
				w.println("function goToPreview(index) { return pp.goToPreview(index); }");
				w.println("function goToPreviousPreview() { return pp.goToPreviousPreview(); }");
				w.println("function goToNextPreview() { return pp.goToNextPreview(); }");
				w.println("function getPreviewer() { return pp; }");
				w.println("//]]></script>");
			}
			w.println("<title>" + XML.xmlEscapeText(pageTitle + " - album view") + "</title>");
			w.println("</head>");
			w.println("<body>");
			
			BaseArguments args = new BaseArguments(getArguments());
			boolean swfFrontAvailable = (getSwfFront() != null);

			w.println("<h2>" + XML.xmlEscapeText(pageTitle) + "</h2>");

			String myUri;
			Component swfComponent = getSwfComponent();
			if( swfFrontAvailable ) {
				if( name == null ) name = getOperandUri();
				myUri = getExternalComponentUri(req, swfComponent, args);
				Crumb c = parseCrumbTrail(myUri, 5, null);
				if( c != null ) {
					w.println("<ul class=\"crumbtrail\">");
					for( ; c != null; c = c.next ) {
						w.println("<li><a href=\""+XML.xmlEscapeAttributeValue(c.uri)+"\">"+
							XML.xmlEscapeText(c.name)+"</a></li>");
					}
					w.println("</ul>");
				}
			} else {
				myUri = null;
			}
				
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
					String ename = e.getName();
					href = processRelativeUri("album", getOperandUri(), href, ename);
					if( CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(e.getTargetType()) ) {
						if( !ename.endsWith("/") ) ename += "/";
					}
					w.write("<tr>");
					w.write("<td><a href=\"" + XML.xmlEscapeAttributeValue(href) + "\">" + XML.xmlEscapeText(ename) + "</a></td>");
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
					href = processRelativeUri("album", getOperandUri(), href);
					String ename = e.getName();
					if( CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(e.getTargetType()) ) {
						if( !ename.endsWith("/") ) ename += "/";
					}
					w.write("<tr>");
					w.write("<td><a href=\"" + XML.xmlEscapeAttributeValue(href) + "\">" + XML.xmlEscapeText(ename) + "</a></td>");
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
					
					w.print("<a onclick=\"return goToPreview("+index+")\" href=\"" + XML.xmlEscapeAttributeValue(getRawUri(imageUri)) + "\">");
					w.print("<img src=\"" + XML.xmlEscapeAttributeValue(getRawUri(shrunkUri)) + "\"/>");
					w.println("</a></div></div>");
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
	
	protected static class AlbumEntryPageGenerator extends PageGenerator {
		Directory.Entry directoryEntry;
		
		public AlbumEntryPageGenerator( Directory.Entry de, Request req ) {
			super( req );
			directoryEntry = de;
		}
		
		public void writeContent(PrintWriter w) {
			UriProcessor rawUriProcessor = BaseUriProcessor.getInstance( req.getContextVars(), "raw" );

			String imageUri = TheGetter.reference(directoryEntry.getTarget(), true, true);
			String previewUri = getPreviewUri(imageUri, req.getContextVars());
			
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
	
	public Response explorifyDirectory( Request req, Directory d ) {
		return getPageGeneratorResult(new AlbumPageGenerator(d, req));
	}
	
	public Response explorifyDirectoryEntry(Request req, Map argumentExpressions, String uri, Directory.Entry de ) {
		return getPageGeneratorResult(new AlbumEntryPageGenerator(de, req ));
	}

	protected String getPathArgumentName() {
		return "operand";
	}
}
