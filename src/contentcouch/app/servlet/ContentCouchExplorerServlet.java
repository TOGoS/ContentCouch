package contentcouch.app.servlet;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import togos.rra.Getter;

import com.eekboom.utils.Strings;

import contentcouch.blob.BlobUtil;
import contentcouch.date.DateUtil;
import contentcouch.file.FileBlob;
import contentcouch.graphics.ImageUtil;
import contentcouch.hashcache.SimpleListFile;
import contentcouch.hashcache.SimpleListFile.Chunk;
import contentcouch.misc.MapUtil;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.SimpleDirectory;
import contentcouch.misc.UriUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.rdf.DcNamespace;
import contentcouch.repository.MetaRepository;
import contentcouch.value.Blob;
import contentcouch.value.Directory;
import contentcouch.value.Ref;
import contentcouch.value.Directory.Entry;
import contentcouch.xml.XML;

public class ContentCouchExplorerServlet extends HttpServlet {
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
	
	class SlfSourcePageGenerator extends PageGenerator {
		protected char[] hexChars = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

		Blob blob;
		String title;
		
		public SlfSourcePageGenerator(Blob b, String title) {
			this.blob = b;
			this.title = title;
		}
		
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
		
		protected void writeIntAsHexLink(PrintWriter w, int i, String title) {
			if( i != 0 ) w.print("<a class=\"chunk-link\" href=\"#chunk-" + intToHex(i) + "\"" + (title == null ? "" : " title=\"" + title + "\"") + ">");
			writeIntAsHex(i, w);
			if( i != 0 ) w.print("</a>");
		}
		
		protected void writeBytesAsHex(byte[] b, PrintWriter w) {
			for( int i=0; i<b.length; ++i ) {
				writeByteAsHex(b[i], w);
				w.write(' ');
			}
		}
		
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
			openSpan("#EBB", w); writeIntAsHexLink(w, c.prevOffset, "Prev"); closeSpan(w);
			openSpan("#BEB", w); writeIntAsHexLink(w, c.nextOffset, "Next"); closeSpan(w);
			openSpan("#EBB", w); writeIntAsHexLink(w, c.listPrevOffset, "Prev in list"); closeSpan(w);
			openSpan("#BEB", w); writeIntAsHexLink(w, c.listNextOffset, "Next in list"); closeSpan(w);
			openSpan("#EEE", w); writeIntAsHex(c.type, w); closeSpan(w);
			if( indexItems != null ) {
				openSpan("#BBE", w); writeIntAsHex(indexItems.length, w); closeSpan(w);
				openSpan("#CCC", w);
				for( int i=0; i<indexItems.length; ++i ) {
					int item = indexItems[i];
					if( item != 0 ) {
						openSpan("#BEB", w);
						writeIntAsHexLink(w, item, null);
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
			SimpleListFile slf = new SimpleListFile(blob, "r");
			Chunk c = slf.getFirstChunk();
			
			w.println("<html>");
			w.println("<head>");
			w.println("<title>" + title + "</title>");
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
	
	class RdfSourcePageGenerator extends PageGenerator {
		String path;
		Blob blob;
		
		public RdfSourcePageGenerator( Blob b, String path ) {
			this.blob = b;
			this.path = path;
		}
		
		protected Pattern RDFRESPAT = Pattern.compile("rdf:resource=\"([^\\\"]+)\"|((?:http:|file:|x-parse-rdf:|data:|urn:)[a-zA-Z0-9\\-\\._\\~:/\\?\\#\\[\\]\\@\\!\\$\\&\\'\\(\\)\\*\\+\\,\\;\\=\\%]+)");
		
		protected String formatLink2(String href, String text) {
			String link = "<a href=\"";
			link += XML.xmlEscapeText(href);
			link += "\">";
			link += XML.xmlEscapeText(text);
			link += "</a>";
			return link; 
		}
		
		protected String formatLink(String url) {
			if( url.startsWith(CcouchNamespace.URI_PARSE_PREFIX) ) {
				// Then show 2 links
				String noParsePart = url.substring(CcouchNamespace.URI_PARSE_PREFIX.length());
				return formatLink2("/explore?uri="+UriUtil.uriEncode(url), CcouchNamespace.URI_PARSE_PREFIX.substring(0,CcouchNamespace.URI_PARSE_PREFIX.length()-1)) + ":" +
					formatLink2("/explore?uri="+UriUtil.uriEncode(noParsePart), noParsePart);
			} else {
				if( url.startsWith("http:") ) {
					return formatLink2(url, url);
				} else {
					int colonIdx = url.indexOf(':');
					int slashIdx = url.indexOf('/');
					if( colonIdx < 0 || (slashIdx > 0 && slashIdx < colonIdx) ) {
						return formatLink2(url, url);
					} else {	
						return formatLink2("/explore?uri="+UriUtil.uriEncode(url), url);
					}
				}
			}
		}
		
		public void write(PrintWriter w) throws IOException {
			w.write("<pre>");
			CharSequence rdf = UTF_8_DECODER.decode(ByteBuffer.wrap(blob.getData(0, (int)blob.getLength())));
			Matcher m = RDFRESPAT.matcher(rdf);
			int at = 0;
			while( m.find() ) {
				w.write(XML.xmlEscapeText(rdf.subSequence(at,m.start()).toString()));
				String url = m.group(1);
				if( url != null ) {
					url = XML.xmlUnescape(url);
					w.write("rdf:resource=\"");
					w.write(formatLink(url));
					w.write("\"");
				} else {
					url = m.group(2);
					url = XML.xmlUnescape(url);
					w.write(formatLink(url));
				}
				at = m.end();
			}
			w.write(XML.xmlEscapeText(rdf.subSequence(at,rdf.length()).toString()));
			w.write("</pre>");
		}
	}
	
	class DirectoryPageGenerator extends PageGenerator {
		final String path;
		final Directory dir;
		public String title;

		public DirectoryPageGenerator(String path, Directory dir ) {
			this.path = path;
			this.dir = dir;
			this.title = (String)MapUtil.getMetadataFrom(dir, DcNamespace.DC_TITLE);
		}
		
		protected String getHref(String path) {
			if( this.path != null && PathUtil.isUri(this.path) ) {
				path = PathUtil.appendPath(this.path, path);
			}
			if( PathUtil.isUri(path) ) {
				return "/explore?uri=" + UriUtil.uriEncode(path);
			} else {
				return path;
			}
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
			
			String title = this.title;
			if( title == null ) title = "Index of " + path;

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
				if( e.getValue() instanceof Ref && PathUtil.isUri(this.path) ) {
					href = ((Ref)e.getValue()).targetUri;
				} else {
					href = name;
				}
				if( CcouchNamespace.OBJECT_TYPE_DIRECTORY.equals(e.getTargetType()) ) {
					if( !PathUtil.isAbsolute(href) && !href.endsWith("/")) href += "/";
					if( !name.endsWith("/") ) name += "/";
				}
				href = getHref(href);
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
	};

	protected void copyFile( File src, File dest ) throws IOException {
		FileInputStream is = new FileInputStream(src);
		FileOutputStream os = new FileOutputStream(dest);
		try {
			byte[] buf = new byte[512];
			int len;
			while( (len = is.read(buf)) > 0 ) {
				os.write(buf, 0, len);
			}
		} finally {
			is.close();
			os.close();
		}
	}
	
	protected MetaRepository repoCache;
	protected MetaRepository getRepo() {
		if( repoCache == null ) { 
			repoCache = new MetaRepository();
			String webPath = this.getServletContext().getRealPath("");
			File configFile = new File(webPath + "/repo-config");
			File configTemplateFile = new File(webPath + "/repo-config.template");
			if( !configFile.exists() ) {
				try {
					copyFile(configTemplateFile, configFile);
				} catch( IOException e ) {
					throw new RuntimeException("Failed to copy " + configTemplateFile.getPath() + " to " + configFile.getPath(), e);
				}
			}
			repoCache.isMainRepo = true;
			repoCache.registerAsGetterAndIdentifier();
			try {
				repoCache.loadConfig(configFile);
			} catch( IOException e ) {
				throw new RuntimeException("Error while loading repo config " + configFile.getPath(), e);
			}
		}
		return repoCache;
	}
	protected MetaRepository getRepo(String name) {
		return (MetaRepository)getRepo().namedRepositories.get(name);
	}
	
	protected Getter getLocalGetter() {
		return getRepo().getGenericGetter();
	}	
	
	protected long guessLastModified( Blob b ) {
		Date date = (Date)MapUtil.getMetadataFrom(b, DcNamespace.DC_MODIFIED);
		if( date != null ) return date.getTime();
		
		if( b instanceof FileBlob ) {
			return ((FileBlob)b).lastModified();
		}
		
		return 0;
	}
	
	public Object getObject(Object root, String path, String rootPath) {
		while( true ) {
			if( root == null || path == null || "".equals(path) || "/".equals(path) ) {
				return root;
			} else if( root instanceof MetaRepository ) {
				return ((MetaRepository)root).getExplorat(path);
			} else if( root instanceof Getter ) {
				return ((Getter)root).get(path);
			} else if( root instanceof Directory ) {
				String[] parts = path.split("/", 2);
				Object nextRoot;
				Directory.Entry nextPart = (Directory.Entry)((Directory)root).getDirectoryEntry(parts[0]);
				if( nextPart == null ) return null;
				nextRoot = nextPart.getValue();
				root = nextRoot;
				rootPath = rootPath + "/" + parts[0];
				if( parts.length == 1 ) {
					path = null;
				} else {
					path = parts[1];
				}
			}
		}
	}

	public Object getObject(String path) {
		SimpleDirectory sd = new SimpleDirectory(getRepo().namedRepositories);
		sd.putMetadata(DcNamespace.DC_TITLE, "All named repositories");
		Object obj = getObject(sd, path, "");
		if( obj != null ) return obj;
		return getLocalGetter().get(path);
	}
	
	public Object getGenericResponse( Object obj, String path ) {
		if( obj instanceof MetaRepository && !(obj instanceof Directory) ) {
			obj = ((MetaRepository)obj).getExplorat("");
		}
		if( obj instanceof Directory ) {
			obj = new DirectoryPageGenerator( path, (Directory)obj );
		}
		if( obj instanceof BufferedImage ) {
			obj = ImageUtil.serializeImage( (BufferedImage)obj, "png", null );
		}
		return obj;
	}

	protected Object exploreObject( Object obj, String path ) {
		if( obj instanceof Blob ) {
			Blob b = (Blob)obj;
			String ct = MetadataUtil.guessContentType(b);
			if( MetadataUtil.CT_SLF.equals(ct) ) {
				return new SlfSourcePageGenerator(b, path);
			} else if( MetadataUtil.CT_RDF.equals(ct) ) {
				return new RdfSourcePageGenerator((Blob)obj, path);
			}
		}
		return getGenericResponse( obj, path );
	}

	public Object explore( String path ) {		
		return exploreObject( getObject(path), path );
	}

	protected Object rawObject( Object obj, String path ) {
		if( obj instanceof Blob ) {
			return obj;
		} else {
			return getGenericResponse(obj, path);
		}
	}
	
	protected Object raw( String path ) {
		return rawObject( getObject(path), path );
	}
	
	public Object get(String path, HttpServletRequest request) {
		if( path.equals("") ) {
			path = "_index";
		}
		if( path.startsWith("explore/") ) {
			return explore(path.substring(8) );
		} else if( path.equals("explore") ) {
			return explore((String)request.getParameter("uri") );
		} else if( path.startsWith("raw/") ) {
			return raw(path.substring(4) );
		} else if( path.equals("raw") ) {
			return raw((String)request.getParameter("uri") );
		} else {
			String webPath = this.getServletContext().getRealPath("");
			File f;
			
			f = new File(webPath + "/" + path);
			if( f.exists() ) { return new FileBlob(f); }
			
			f = new File(webPath + "/" + path + ".html");
			if( f.exists() ) { return new FileBlob(f); }
			
			return "I don't know about '" + path + "'";
		}
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String pi = request.getPathInfo();
		if( pi == null ) pi = request.getRequestURI();
		if( pi == null ) pi = "/";
		
		try {
			Object page = get(pi.substring(1), request);
			if( page == null ) page = "Nothing found!";
			if( page instanceof HttpServletRequestHandler ) {
				((HttpServletRequestHandler)page).handle(request, response);
			} else if( page instanceof Blob ) {
				String contentType = guessContentType((Blob)page);
				if( contentType != null ) response.setHeader("Content-Type", contentType);
				else response.setHeader("Content-Type", "");
				
				long mtime = guessLastModified((Blob)page);
				if( mtime > 0 ) response.setDateHeader("Last-Modified", mtime);
				
				BlobUtil.writeBlobToOutputStream(((Blob)page), response.getOutputStream());
			} else {
				response.setHeader("Content-Type", "text/plain");			
				response.getWriter().println(page.toString());
			}
		} catch( RuntimeException e ) {
			response.setHeader("Content-Type", "text/plain");
			e.printStackTrace(response.getWriter());
		}
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doGet(request, response);
	}
}
