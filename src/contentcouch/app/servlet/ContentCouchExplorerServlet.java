package contentcouch.app.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import contentcouch.app.ContentCouchRepository;
import contentcouch.data.Blob;
import contentcouch.data.BlobUtil;
import contentcouch.data.Directory;
import contentcouch.data.FileBlob;
import contentcouch.data.Directory.Entry;
import contentcouch.hashcache.SimpleListFile;
import contentcouch.hashcache.SimpleListFile.Chunk;
import contentcouch.xml.RDF;
import contentcouch.xml.XML;
import contentcouch.xml.RDF.Ref;

public class ContentCouchExplorerServlet extends HttpServlet {
	protected ContentCouchRepository getRepo() {
		return new ContentCouchRepository("junk-repo");
	}
	
	public interface HttpServletRequestHandler {
		public void handle( HttpServletRequest request, HttpServletResponse response ) throws IOException;
	}
	
	public abstract class PageGenerator implements HttpServletRequestHandler {
		public String getContentType() {
			return "text/html; charset=utf-8";
		}
		public abstract void write(PrintWriter w) throws IOException;
		public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
			response.setContentType(getContentType());
			write(response.getWriter());
		}
	}

	public Object explore(final String path) {
		Object obj = getRepo().get(path);
		if( obj instanceof FileBlob && ((FileBlob)obj).getFile().getName().endsWith(".slf") ) {
			final File file = ((FileBlob)obj).getFile();
			return new PageGenerator() {
				protected char[] hexChars = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
				
				protected void writeByteAsHex(byte b, PrintWriter w) {
					w.write(hexChars[(b>>4)&0xF]);
					w.write(hexChars[(b>>0)&0xF]);
				}
				
				protected String intToHex(int i) {
					return "" +
						hexChars[(i >> 28)&0xF] +
						hexChars[(i >> 24)&0xF] +
						hexChars[(i >> 20)&0xF] +
						hexChars[(i >> 16)&0xF] +
						hexChars[(i >> 12)&0xF] +
						hexChars[(i >>  8)&0xF] +
						hexChars[(i >>  4)&0xF] +
						hexChars[(i >>  0)&0xF];
				}
				
				protected void writeIntAsHex(int i, PrintWriter w) {
					writeByteAsHex((byte)((i >> 24) & 0xFF), w);
					w.write(',');
					writeByteAsHex((byte)((i >> 16) & 0xFF), w);
					w.write(',');
					writeByteAsHex((byte)((i >>  8) & 0xFF), w);
					w.write(',');
					writeByteAsHex((byte)((i >>  0) & 0xFF), w);
					w.write(' ');
				}
				
				protected void writeIntAsHexLink(int i, PrintWriter w) {
					if( i != 0 ) w.print("<a class=\"chunk-link\" href=\"#chunk-" + intToHex(i) + "\">");
					writeIntAsHex(i, w);
					if( i != 0 ) w.print("</a>");
				}
				
				protected void writeBytesAsHex(byte[] b, PrintWriter w) {
					for( int i=0; i<b.length; ++i ) {
						writeByteAsHex(b[i], w);
						w.write(' ');
					}
				}
				
				protected CharsetDecoder UTF_8_DECODER = Charset.forName("UTF-8").newDecoder();
				
				protected void writeBytesAsString(byte[] b, PrintWriter w) {
					try {
						String s = UTF_8_DECODER.decode(ByteBuffer.wrap(b)).toString();
						w.print("<b>");
						w.print(s);
						w.print("</b>");
					} catch( CharacterCodingException e ) {
						writeBytesAsHex(b, w);
					}
				}

				protected void openSpan(String color, Writer w) throws IOException {
					w.write("<span style=\"background-color:" + color + "\">");
				}
				protected void closeSpan(Writer w) throws IOException {
					w.write("</span>");
				}
				
				protected void writeChunkAsHtml(SimpleListFile slf, Chunk c, PrintWriter w) throws IOException {
					w.println("<div class=\"chunk\">");
					w.print("<h4 class=\"chunk-title\" id=\"chunk-" + intToHex(c.offset) + "\">" +
							intToHex(c.offset) + " - " + SimpleListFile.intToStr(c.type) + "</h4>");
					
					int[] indexItems = null;
					int filledItems = 0;
					
					if( c.type == SimpleListFile.CHUNK_TYPE_PAIR ) {
						w.println("<div class=\"chunk-summary\">");
						byte[] key = slf.getPairKey(c.offset);
						byte[] value = slf.getPairValue(c.offset);
						writeBytesAsString(key, w);
						w.print(" = ");
						writeBytesAsString(value, w);
						w.println("</div");
					} else if( c.type == SimpleListFile.CHUNK_TYPE_INDX ) {
						indexItems = slf.getIndexItems(c);
						for( int i=0; i<indexItems.length; ++i ) {
							if( indexItems[i] != 0 ) {
								++filledItems;
							}
						}
						w.println("<div class=\"chunk-summary\">");
						w.print("<b>" + indexItems.length + "</b> items, <b>" + filledItems + "</b> filled");
						w.println("</div");
					}
					w.println("<div class=\"chunk-content\">");
					openSpan("#EBB", w); writeIntAsHexLink(c.prevOffset, w); closeSpan(w);
					openSpan("#BEB", w); writeIntAsHexLink(c.nextOffset, w); closeSpan(w);
					openSpan("#EBB", w); writeIntAsHexLink(c.listPrevOffset, w); closeSpan(w);
					openSpan("#BEB", w); writeIntAsHexLink(c.listNextOffset, w); closeSpan(w);
					openSpan("#EEE", w); writeIntAsHex(c.type, w); closeSpan(w);
					if( indexItems != null ) {
						openSpan("#BBE", w); writeIntAsHex(indexItems.length, w); closeSpan(w);
						openSpan("#CCC", w);
						for( int i=0; i<indexItems.length; ++i ) {
							int item = indexItems[i];
							if( item != 0 ) {
								openSpan("#BEB", w);
								writeIntAsHexLink(item, w);
								closeSpan(w);
							} else {
								writeIntAsHex(item, w);
							}
						}
						closeSpan(w);
					} else {
						openSpan("#CCC", w); writeBytesAsHex(slf.getChunkData(c), w); closeSpan(w);
					}
					w.println("</div>");
					w.println("</div>");
					
				}
				
				public void write(PrintWriter w) throws IOException {
					SimpleListFile slf = new SimpleListFile(file, "r");
					Chunk c = slf.getFirstChunk();
					
					w.println("<html>");
					w.println("<head>");
					w.println("<title>" + file.getName() + "</title>");
					w.println("<style>");
					w.println("body { background-color: #88B; color: black; }");
					w.println(".chunk-title { margin: 0; padding: 2px 4px 2px 4px; background-color: #EEE }");
					w.println(".chunk-summary { margin: 0; padding: 2px 4px 2px 12px; color: #808; background-color: #FFF }");
					w.println(".chunk-content { margin: 0; padding: 1px 4px 1px 4px }");
					w.println(".chunk-content > span, .chunk-summary > span { margin: 0; padding: 1px 0px 1px 0px; }");
					w.println(".chunk-title, .chunk-content, .chunk-summary { font-family: monospace }");
					w.println(".chunk { border:1px solid; margin:4px; padding: 0px; background-color:silver }");
					w.println(".chunk-link { text-decoration: none }");
					w.println("</style>");
					w.println("</head>");
					w.println("<body>");
					while( c != null ) {
						writeChunkAsHtml(slf, c, w);
						c = slf.getChunk(c.nextOffset);
					}
					slf.close();
					w.println("</body>");
					w.println("</html>");
				}				
			};
		} else if( obj instanceof Directory ) {			
			final Directory dir = (Directory)obj;
			return new PageGenerator() {
				public void write(PrintWriter w) throws IOException {
					Map entries = dir.getEntries();
					ArrayList entryList = new ArrayList(entries.values());
					Collections.sort(entryList, new Comparator() {
						public int compare(Object o1, Object o2) {
							Entry e1 = (Entry)o1;
							Entry e2 = (Entry)o2;
							if( e1.getTargetType().equals(e2.getTargetType()) ) {
								return e1.getName().compareTo(e2.getName());
							} else {
								if( RDF.OBJECT_TYPE_DIRECTORY.equals(e1.getTargetType()) ) {
									return -1;
								} else {
									return 1;
								}
							}
						}
					});
					
					String title = "Index of " + path;

					w.println("<html>");
					w.println("<head>");
					w.println("<title>" + XML.xmlEscapeText(title) + "</title>");
					w.println("</head>");
					w.println("<body>");
					w.println("<h2>" + XML.xmlEscapeText(title) + "</h2>");
					w.println("<ul>");
					for( Iterator i=entryList.iterator(); i.hasNext(); ) {
						Entry e = (Entry)i.next();
						String href;
						String name = e.getName(); 
						if( e.getTarget() instanceof Ref ) {
							href = "/explore/" + (((Ref)e.getTarget()).targetUri);
						} else {
							href = name;
						}
						if( RDF.OBJECT_TYPE_DIRECTORY.equals(e.getTargetType()) ) {
							href += "/";
							name += "/";
						}
						w.println("<li><a href=\"" + XML.xmlEscapeAttributeValue(href) + "\">" + XML.xmlEscapeText(name) + "</a></li>");
					}
					w.println("</ul>");
					w.println("</body>");
					w.println("</html>");
				}
			};
		} else {
			return obj;
		}
	}
	
	public Object get(String path) {
		if( path.startsWith("explore/") ) {
			return explore(path.substring(8));
		} else {
			return "I don't know about '" + path + "'";
		}
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setHeader("Content-Type", "text/plain");
		
		String pi = request.getPathInfo();
		if( pi == null ) pi = request.getRequestURI();
		if( pi == null ) pi = "/";
		
		Object page = get(pi.substring(1));
		if( page == null ) page = "Nothing found!";
		if( page instanceof HttpServletRequestHandler ) {
			((HttpServletRequestHandler)page).handle(request, response);
		} else if( page instanceof Blob ) {
			BlobUtil.writeBlobToOutputStream(((Blob)page), response.getOutputStream());
		} else {
			response.getWriter().println(page.toString());
		}
	}
}
