package contentcouch.explorify;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import contentcouch.misc.Function1;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Blob;
import contentcouch.xml.XML;

public class RdfSourcePageGenerator extends PageGenerator {
	Blob blob;
	
	public RdfSourcePageGenerator( Blob b, Function1 uriProcessor ) {
		this.blob = b;
		this.uriProcessor = uriProcessor;
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
	
	protected String formatLink( String url ) {
		if( url.startsWith(CcouchNamespace.URI_PARSE_PREFIX) ) {
			// Then show 2 links
			String noParsePart = url.substring(CcouchNamespace.URI_PARSE_PREFIX.length());
			return formatLink2(processUri(url), CcouchNamespace.URI_PARSE_PREFIX.substring(0,CcouchNamespace.URI_PARSE_PREFIX.length()-1)) + ":" +
				formatLink2(processUri(noParsePart), noParsePart);
		} else {
			return formatLink2(processUri(url), url);
		}
	}
	
	public void write(PrintWriter w) throws IOException {
		w.write("<pre>");
		CharSequence rdf = ValueUtil.UTF_8_DECODER.decode(ByteBuffer.wrap(blob.getData(0, (int)blob.getLength())));
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