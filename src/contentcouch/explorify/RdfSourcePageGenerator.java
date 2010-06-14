package contentcouch.explorify;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.value.Blob;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.xml.XML;

public class RdfSourcePageGenerator extends CCouchExplorerPageGenerator {
	Blob blob;
	
	public RdfSourcePageGenerator( Request req, Response subRes, Blob b ) {
		super( req, subRes );
		this.blob = b;
	}
	
	protected Pattern RDFRESPAT = Pattern.compile("rdf:resource=\"([^\\\"]+)\"|((?:http:|file:|x-rdf-subject:|x-parse-rdf:|data:|urn:|active:|x-ccouch-head:)[a-zA-Z0-9\\-\\._\\~:/\\?\\#\\[\\]\\@\\!\\$\\&\\'\\(\\)\\*\\+\\,\\;\\=\\%]+)");
	
	protected String formatLink2(String href, String text) {
		String link = "<a href=\"";
		link += XML.xmlEscapeText(href);
		link += "\">";
		link += XML.xmlEscapeText(text);
		link += "</a>";
		return link; 
	}
	
	protected String formatLink( String url ) {
		if( url.startsWith(CcouchNamespace.RDF_SUBJECT_URI_PREFIX) ) {
			// Then show 2 links
			String noParsePart = url.substring(CcouchNamespace.RDF_SUBJECT_URI_PREFIX.length());
			return formatLink2(getExternalUri(url), CcouchNamespace.RDF_SUBJECT_URI_PREFIX.substring(0,CcouchNamespace.RDF_SUBJECT_URI_PREFIX.length()-1)) + ":" +
				formatLink2(getExternalUri(noParsePart), noParsePart);
		} else if( url.startsWith(CcouchNamespace.RDF_SUBJECT_URI_PREFIX_OLD) ) {
			// Then show 2 links
			String noParsePart = url.substring(CcouchNamespace.RDF_SUBJECT_URI_PREFIX_OLD.length());
			return formatLink2(getExternalUri(url), CcouchNamespace.RDF_SUBJECT_URI_PREFIX_OLD.substring(0,CcouchNamespace.RDF_SUBJECT_URI_PREFIX_OLD.length()-1)) + ":" +
				formatLink2(getExternalUri(noParsePart), noParsePart);
		} else {
			return formatLink2(getExternalUri(url), url);
		}
	}
	
	public void writeContent(PrintWriter w) {
		w.println("<div class=\"main-content\">");
		w.println("<pre class=\"source\">");
		CharSequence rdf;
		try {
			rdf = ValueUtil.UTF_8_DECODER.decode(ByteBuffer.wrap(blob.getData(0, (int)blob.getLength())));
		} catch( CharacterCodingException e ) {
			throw new RuntimeException(e);
		}
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
		w.println("</pre>");
		w.println("</div>");
	}
}