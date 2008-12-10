package contentcouch.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RDF {
	public static String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static String DC_NS  = "http://purl.org/dc/elements/1.1/";
	
	public static String RDF_ABOUT       = RDF_NS + "about";
	public static String RDF_RESOURCE    = RDF_NS + "resource";
	public static String RDF_DESCRIPTION = RDF_NS + "Description";
	
	public static String DC_CREATOR = DC_NS + "creator";	
	
	static Map standardNsAbbreviations = new HashMap();
	static {
		standardNsAbbreviations.put("rdf", RDF_NS);
		standardNsAbbreviations.put("dc", DC_NS);
		standardNsAbbreviations.put("xmlns", "http://www.w3.org/2000/xmlns/");
	}
	
	static class Ref {
		public String targetUri;
		public Ref(String targetUri) {
			this.targetUri = targetUri;
		}
	}
	
	static class Description extends HashMap {
		public Ref about;
		public Description() {
			super();
		}
	}
	
	public static String xmlEscapeText( String text ) {
		return text.replace("&","&amp").replace("<", "&lt;").replace(">","&gt;");
	}
	
	public static String xmlEscapeName( String text ) {
		return text;
	}

	public static String xmlEscapeAttributeValue( String text ) {
		return text.replace("&","&amp").replace("\"","&quot;").replace("<", "&lt;").replace(">","&gt;");
	}

	public static String longToShort( String name, Map availableNsAbbreviations, Map usedNsAbbreviations ) {
		for( Iterator i=usedNsAbbreviations.keySet().iterator(); i.hasNext(); ) {
			String nsShort = (String)i.next();
			String nsLong = (String)usedNsAbbreviations.get(nsShort);
			if( name.length() > nsLong.length() && name.startsWith(nsLong) ) {
				String postfix = name.substring(nsLong.length());
				return nsShort + ":" + postfix;
			}
		}
		for( Iterator i=availableNsAbbreviations.keySet().iterator(); i.hasNext(); ) {
			String nsShort = (String)i.next();
			String nsLong = (String)availableNsAbbreviations.get(nsShort);
			if( name.length() > nsLong.length() && name.startsWith(nsLong) ) {
				String postfix = name.substring(nsLong.length());
				usedNsAbbreviations.put(nsShort,nsLong);
				return nsShort + ":" + postfix;
			}
		}
		// Otherwise, we need to make one up!
		int lo = name.lastIndexOf('#');
		if( lo == -1 ) lo = name.lastIndexOf('/');
		if( lo < 1 || lo == name.length()-1 ) {
			throw new RuntimeException("Can't generate namespace for '" + name + '"');
		}
		String nsLong = name.substring(0,lo+1);
		String postfix = name.substring(lo+1);
		int nsShortPostfix = 0;
		String nsShort;
		while( usedNsAbbreviations.containsKey(nsShort = "a"+nsShortPostfix) ) {
			nsShortPostfix += 1;
		}
		usedNsAbbreviations.put(nsShort, nsLong);
		return nsShort + ":" + postfix;
	}
	
	public static String shortToLong( String name, Map nsAbbreviations ) {
		int lo = name.indexOf(':');
		String nsShort, subName;
		if( lo == -1 ) {
			nsShort = "";
			subName = name;
		} else if( lo < 1 || lo == name.length()-1 ) {
			throw new RuntimeException("Can't parse '" + name + "' as namespace name + postfix");
		} else {
			nsShort = name.substring(0,lo);
			subName = name.substring(lo+1);
		}
		String nsLong = (String)nsAbbreviations.get(nsShort);
		if( nsLong == null ) {
			if( "".equals(nsShort) ) {
				throw new RuntimeException("No default namespace for '" + name + "'");
			} else {
				throw new RuntimeException("Unknown namespace name '" + nsShort + "'");
			}
		}
		return nsLong + subName;
	}
	
	public static void writeRdfProperties( Writer w, Map properties, String padding, Map usednsAbbreviations )
		throws IOException
	{
		for( Iterator propIter = properties.keySet().iterator(); propIter.hasNext(); ) {
			String propName = (String)propIter.next();
			Object value = properties.get(propName);
			String propNodeName = longToShort(propName, standardNsAbbreviations, usednsAbbreviations);
			if( value instanceof Ref ) {
				w.write(padding + "<" + propNodeName + " rdf:resource=\"" + xmlEscapeAttributeValue(((Ref)value).targetUri) + "\"/>\n");
			} else if( value instanceof String ) {
				w.write(padding + "<" + propNodeName + ">" + xmlEscapeText((String)value) + "</" + propNodeName + ">\n");
			} else if( value instanceof Map ) {
				w.write(padding + "<" + propNodeName + ">\n");
				usednsAbbreviations.put("rdf", standardNsAbbreviations.get("rdf"));
				w.write(padding + "\t<rdf:Description>\n");
				writeRdfProperties( w, (Map)value, padding + "\t\t", usednsAbbreviations);
				w.write(padding + "\t</rdf:Description>\n");
				w.write(padding + "</" + propNodeName + ">\n");
			}
		}
	}
	
	public static void writeXmlns( Writer w, Map nsAbbreviations )
		throws IOException
	{
		for( Iterator i=nsAbbreviations.keySet().iterator(); i.hasNext(); ) {
			String nsShort = (String)i.next();
			String nsLong = (String)nsAbbreviations.get(nsShort);
			w.write(" xmlns:" + nsShort + "=\"" + xmlEscapeAttributeValue(nsLong) + "\"");
		}
	}
	
	public static String xmlEncodeRdf( Object value ) {
		try {
			if( value instanceof Description ) {
				Description desc = (Description)value;
				
				Writer outerWriter = new StringWriter();
				Writer subWriter = new StringWriter();
				Map usednsAbbreviations = new HashMap();
				usednsAbbreviations.put("rdf", standardNsAbbreviations.get("rdf"));
				writeRdfProperties( subWriter, desc, "\t", usednsAbbreviations );
				outerWriter.write( "<rdf:Description" );
				writeXmlns( outerWriter, usednsAbbreviations );
				if( desc.about != null ) {
					outerWriter.write(" rdf:about=\"" + xmlEscapeAttributeValue(desc.about.targetUri) + "\"");
				}
				outerWriter.write( ">\n" );
				outerWriter.write( subWriter.toString() );
				outerWriter.write( "</rdf:Description>\n" );
				return outerWriter.toString();
			} else {
				return value.toString();
			}
		} catch( IOException e ) {
			throw new RuntimeException( "Error while generating xml", e );
		}
	}
	
	static final class ParseResult {
		public Object value;
		public int newOffset;
		public ParseResult( Object value, int newOffset ) {
			this.value = value;
			this.newOffset = newOffset;
		}
	}
	
	final static class XmlAttribute {
		public String name;
		public String value;
		public XmlAttribute( String name, String value ) {
			this.name = name;
			this.value = value;
		}
	}
	final static class XmlCloseTag {
		public String name;
		public XmlCloseTag(String name) {
			this.name = name;
		}
	}
	final static class XmlOpenTag {
		public String name;
		public Map attributes = new HashMap();
		public boolean closed;

		public XmlOpenTag( String name, boolean closed, Map attributes ) {
			this.name = name;
			this.attributes = attributes;
			this.closed = closed;
		}
	}
	
	public static ParseResult parseXmlText( char[] chars, int offset, char end ) {
		char[] text = new char[chars.length];
		int textLength = 0;
		while( offset < chars.length && chars[offset] != end ) {
			if( chars[offset] == '&' ) {
				char textChar;
				switch( chars[offset+1] ) {
				case( 'a' ): textChar = '&'; offset += 5; break; // &amp;
				case( 'l' ): textChar = '<'; offset += 4; break; // &lt;
				case( 'g' ): textChar = '>'; offset += 4; break; // &gt;
				case( 'q' ): textChar = '"'; offset += 6; break; // &quot;
				default    : textChar = '&'; offset += 1;
				}
				text[textLength++] = textChar;
			} else {
				text[textLength++] = chars[offset++];
			}
		}
		return new ParseResult( new String(text, 0, textLength), offset );
	}
	
	public static int skipWhitespace( char[] chars, int offset ) {
		while( offset < chars.length ) {
			switch( chars[offset] ) {
			case( ' ' ): case( '\t' ): case( '\r' ): case( '\n' ):
				++offset; break;
			default:
				return offset;
			}
		}
		return offset;
	}
	
	public static ParseResult parseXmlAttribute( char[] chars, int offset ) {
		offset = skipWhitespace(chars, offset);
		switch( chars[offset] ) {
		case( '>' ): case( '/' ): return new ParseResult(null, offset);
		}
		ParseResult nameParseResult = parseXmlText(chars, offset, '=');
		String name = (String)nameParseResult.value;
		offset = nameParseResult.newOffset;
		if( chars[offset++] != '=' ) throw new RuntimeException("Expected '=' but found '" + chars[offset] + "' while parsing XML attribute");
		if( chars[offset++] != '"' ) throw new RuntimeException("Expected '\"' but found '" + chars[offset] + "' while parsing XML attribute value opening");
		ParseResult valueParseResult = parseXmlText(chars, offset, '"');
		String value = (String)valueParseResult.value;
		offset = valueParseResult.newOffset;
		if( chars[offset++] != '"' ) throw new RuntimeException("Expected '\"' but found '" + chars[offset] + "' while parsing XML attribute value closing");
		return new ParseResult( new XmlAttribute(name, value), offset );
	}
		
	public static ParseResult parseXmlTag( char[] chars, int offset ) {
		if( chars[offset++] != '<' ) throw new RuntimeException("Cannot parse a tag that doesn't start with ','");
		StringBuilder nameBuilder = new StringBuilder();
		
		boolean isCloseTag = (chars[offset] == '/');
		if( isCloseTag ) ++offset;
		
		// Read name
		nameloop: while( offset < chars.length ) {
			switch( chars[offset] ) {
			case( ' ' ): case( '\t' ): case( '\r' ): case( '\n' ):
			case( '>' ): case( '"' ): case( '/' ):
				break nameloop;
			default:
				nameBuilder.append( chars[offset++] );
			}
		}
		String name = nameBuilder.toString();
		
		if( isCloseTag ) {
			offset = skipWhitespace(chars, offset);
			return new ParseResult( new XmlCloseTag(name), offset+1 );
		}
		
		Map attributes = new HashMap();
		XmlAttribute attr;
		do {
			ParseResult attrParseResult = parseXmlAttribute(chars, offset);
			offset = attrParseResult.newOffset;
			attr = (XmlAttribute)attrParseResult.value; 
			if( attr != null ) {
				attributes.put( attr.name, attr.value );
			}
		} while( attr != null );
		
		if( offset >= chars.length ) throw new RuntimeException("Unexpectedly reached end of XML in middle of tag");
		switch( chars[offset] ) {
		case( '/' ): return new ParseResult( new XmlOpenTag(name, true, attributes), offset+2 ); 
		case( '>' ): return new ParseResult( new XmlOpenTag(name, false, attributes), offset+1 );
		default:
			throw new RuntimeException("Expected '/' or '>' but found '" + chars[offset] + "' while reading end of XML tag");
		}
	}
	
	public static ParseResult parseXmlPart( char[] chars, int offset ) {
		int c;
		whitespaceloop: while( offset < chars.length ) {
			// Skip whitespace
			c = chars[offset];
			switch( c ) {
			case( '<' ):
				break whitespaceloop;
			case( ' ' ): case( '\t' ): case( '\r' ): case( '\n' ):
				++offset;
				break;
			default:
				ParseResult r = parseXmlText( chars, offset, '<' );
				r.value = ((String)r.value).trim();
				return r;
			}
		}
		if( offset < chars.length ) {
			return parseXmlTag( chars, offset );
		}
		return new ParseResult(null, offset);
	}
	
	public static Map updateNamespaces( Object xmlPart, Map previousNsAbbreviations ) {
		if( !(xmlPart instanceof XmlOpenTag) ) return previousNsAbbreviations;
		XmlOpenTag openTag = (XmlOpenTag)xmlPart;
		Map newNsAbbreviations = previousNsAbbreviations;
		for( Iterator i=openTag.attributes.keySet().iterator(); i.hasNext(); ) {
			String attrKey = (String)i.next();
			String attrValue = (String)openTag.attributes.get(attrKey);
			if( "xmlns".equals(attrKey) ) {
				if( newNsAbbreviations == previousNsAbbreviations ) newNsAbbreviations = new HashMap(previousNsAbbreviations); 
				newNsAbbreviations.put( "", attrValue );
			} else	if( attrKey.startsWith("xmlns:") ) {
				if( newNsAbbreviations == previousNsAbbreviations ) newNsAbbreviations = new HashMap(previousNsAbbreviations);
				newNsAbbreviations.put( attrKey.substring(6), attrValue );
			}
		}
		return newNsAbbreviations; 
	}
	
	public static Object namespaceXmlPart( Object xmlPart, Map nsAbbreviations ) {
		if( xmlPart instanceof XmlCloseTag ) {
			return new XmlCloseTag(shortToLong(((XmlCloseTag)xmlPart).name, nsAbbreviations));
		} else if( xmlPart instanceof XmlOpenTag ) {
			XmlOpenTag oldOpenTag = (XmlOpenTag)xmlPart;
			Map attributes;
			if( oldOpenTag.attributes.size() > 0 ) {
				attributes = new HashMap();
				for( Iterator i=oldOpenTag.attributes.keySet().iterator(); i.hasNext(); ) {
					String name = (String)i.next();
					attributes.put( shortToLong(name, nsAbbreviations), oldOpenTag.attributes.get(name));
				}
			} else {
				attributes = oldOpenTag.attributes; 
			}
			return new XmlOpenTag( shortToLong(oldOpenTag.name, nsAbbreviations), oldOpenTag.closed, attributes );
		} else {
			return xmlPart;
		}
	}
	
	public static ParseResult parseRdf( char[] chars, int offset, Map nsAbbreviations ) {
		ParseResult xmlParseResult = parseXmlPart(chars, offset);
		Object xmlPart = xmlParseResult.value;
		
		if( xmlPart instanceof XmlOpenTag ) {
			offset = xmlParseResult.newOffset;
			Description desc = new Description();
			
			XmlOpenTag descOpenTag = (XmlOpenTag)xmlPart;
			Map descNsAbbreviatios = updateNamespaces( descOpenTag, nsAbbreviations );
			descOpenTag = (XmlOpenTag)namespaceXmlPart(descOpenTag, descNsAbbreviatios);
			
			if( !RDF_DESCRIPTION.equals(descOpenTag.name) ) {
				throw new RuntimeException("Can't parse + '" + descOpenTag.name + "' as RDF");
			}
			
			if( descOpenTag.closed ) {
				return new ParseResult( desc, offset );
			}
			
			while( true ) {
				xmlParseResult = parseXmlPart(chars, offset);
				xmlPart = xmlParseResult.value;
				Map predicateNsAbbreviations = updateNamespaces(xmlPart, descNsAbbreviatios);
				xmlPart = namespaceXmlPart(xmlPart, predicateNsAbbreviations);
				
				if( xmlPart instanceof XmlCloseTag ) {
					if( !descOpenTag.name.equals(((XmlCloseTag)xmlPart).name) ) {
						throw new RuntimeException("Start and end tags do not match: " + descOpenTag.name + " != " + ((XmlCloseTag)xmlPart).name);
					}
					return new ParseResult( desc, xmlParseResult.newOffset );
				} else if( xmlPart instanceof XmlOpenTag ) {
					XmlOpenTag predicateOpenTag = (XmlOpenTag)xmlPart;
					offset = xmlParseResult.newOffset;
					
					String resourceUri = (String)predicateOpenTag.attributes.get("rdf:resource");
					if( resourceUri != null ) {
						desc.put(predicateOpenTag.name, new Ref(resourceUri));
					}
					
					if( !predicateOpenTag.closed ) {					
						while( true ) {
							ParseResult rdfValueParseResult = parseRdf(chars, offset, predicateNsAbbreviations);
							offset = rdfValueParseResult.newOffset;
							if( rdfValueParseResult.value == null ) {
								break;
							}
							Object value = rdfValueParseResult.value;
							desc.put(predicateOpenTag.name, value);
						}
						ParseResult predicateCloseTagParseResult = parseXmlPart(chars, offset);
						if( !(predicateCloseTagParseResult.value instanceof XmlCloseTag) ) {
							throw new RuntimeException("Expected XML close tag but found " + predicateCloseTagParseResult.value.getClass().getName() );
						}
						XmlCloseTag predicateCloseTag = (XmlCloseTag)predicateCloseTagParseResult.value;
						predicateCloseTag = (XmlCloseTag)namespaceXmlPart(predicateCloseTag, predicateNsAbbreviations);
						if( !predicateCloseTag.name.equals(predicateOpenTag.name) ) {
							throw new RuntimeException("Start and end predicate tags do not match: " + predicateOpenTag.name + " != " + predicateCloseTag.name );
						}
						offset = predicateCloseTagParseResult.newOffset;
					}
				} else {
					offset = xmlParseResult.newOffset;
					// somehow report unrecognised element?
				}
			}
		} else if( xmlPart instanceof XmlCloseTag ) {
			return new ParseResult( null, offset );
		} else {
			return new ParseResult( xmlPart, xmlParseResult.newOffset );
		}
	}
	
	public static Object rdfToMap( String rdf ) {
		char[] chars = new char[rdf.length()];
		rdf.getChars(0, chars.length, chars, 0);
		Map nsAbbreviations = standardNsAbbreviations;
		ParseResult rdfParseResult = parseRdf( chars, 0, nsAbbreviations );
		return rdfParseResult.value;
	}
	
	public static byte[] readFile( String filename )
		throws IOException
	{
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(file);
		int read = 0;
		int length = (int)file.length();
		byte[] content = new byte[length];
		while( read < length ) {
			read += fis.read(content, read, length-read );
		}
		return content;
	}
	
	public static void main( String[] args ) {
		/*		
		Description props = new Description();
		props.about = new Ref("http://www.nuke24.net/about");
		props.put(DC_CREATOR, "TOGoS");
		Description subProps = new Description();
		subProps.put("junk/extraprop", "This is an extra property");
		props.put("junk/has-more-props", subProps);
		props.put("junk/part-of-site", new Ref("http://www.nuke24.net/"));
		String rdf = RDF.xmlEncodeRdf(props);
		*/

		String rdf;
		try {
			rdf = new String(readFile("junk/parser-test.rdf"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		System.out.println("Input: " + rdf);
		
		Object parsed = rdfToMap(rdf);
		rdf = RDF.xmlEncodeRdf(parsed);
		System.out.println("Output: " + rdf);
	}
}
