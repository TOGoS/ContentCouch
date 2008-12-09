package contentcouch.metadata;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RDF {
	static Map standardPrefixes = new HashMap();
	static {
		standardPrefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		standardPrefixes.put("dc", "http://purl.org/dc/elements/1.1/");
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

	public static String longToShort( String name, Map prefixes, Map usedPrefixes ) {
		for( Iterator i=usedPrefixes.keySet().iterator(); i.hasNext(); ) {
			String nsShort = (String)i.next();
			String nsLong = (String)usedPrefixes.get(nsShort);
			if( name.startsWith(nsLong) ) {
				String postfix = name.substring(nsLong.length());
				return nsShort + ":" + postfix;
			}
		}
		for( Iterator i=prefixes.keySet().iterator(); i.hasNext(); ) {
			String nsShort = (String)i.next();
			String nsLong = (String)prefixes.get(nsShort);
			if( name.startsWith(nsLong) ) {
				String postfix = name.substring(nsLong.length());
				usedPrefixes.put(nsShort,nsLong);
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
		while( usedPrefixes.containsKey(nsShort = "a"+nsShortPostfix) ) {
			nsShortPostfix += 1;
		}
		usedPrefixes.put(nsShort, nsLong);
		return nsShort + ":" + postfix;
	}
	
	public static String shortToLong( String name, Map prefixes ) {
		int lo = name.indexOf(':');
		if( lo < 1 || lo == name.length()-1 ) {
			throw new RuntimeException("Can't parse '" + name + "' as namespace name + postfix");
		}
		String nsShort = name.substring(0,lo);
		String nsLong = (String)prefixes.get(nsShort);
		if( nsLong == null ) {
			throw new RuntimeException("Unknown namespace name '" + nsShort + "'");
		}
		return nsLong + name.substring(lo+1);
	}
	
	public static void writeRdfProperties( Writer w, Map properties, String padding, Map usedPrefixes )
		throws IOException
	{
		for( Iterator propIter = properties.keySet().iterator(); propIter.hasNext(); ) {
			String propName = (String)propIter.next();
			Object value = properties.get(propName);
			String propNodeName = longToShort(propName, standardPrefixes, usedPrefixes);
			if( value instanceof Ref ) {
				w.write(padding + "<" + propNodeName + " rdf:resource=\"" + xmlEscapeAttributeValue(((Ref)value).targetUri) + "\"/>\n");
			} else if( value instanceof String ) {
				w.write(padding + "<" + propNodeName + ">" + xmlEscapeText((String)value) + "</" + propNodeName + ">\n");
			} else if( value instanceof Map ) {
				w.write(padding + "<" + propNodeName + ">\n");
				usedPrefixes.put("rdf", standardPrefixes.get("rdf"));
				w.write(padding + "\t<rdf:Description>\n");
				writeRdfProperties( w, (Map)value, padding + "\t\t", usedPrefixes);
				w.write(padding + "\t</rdf:Description>\n");
				w.write(padding + "</" + propNodeName + ">\n");
			}
		}
	}
	
	public static void writeXmlns( Writer w, Map prefixes )
		throws IOException
	{
		for( Iterator i=prefixes.keySet().iterator(); i.hasNext(); ) {
			String nsShort = (String)i.next();
			String nsLong = (String)prefixes.get(nsShort);
			w.write(" xmlns:" + nsShort + "=\"" + xmlEscapeAttributeValue(nsLong) + "\"");
		}
	}
	
	public static String xmlEncodeRdf( Object value ) {
		try {
			if( value instanceof Description ) {
				Description desc = (Description)value;
				
				Writer outerWriter = new StringWriter();
				Writer subWriter = new StringWriter();
				Map usedPrefixes = new HashMap();
				usedPrefixes.put("rdf", standardPrefixes.get("rdf"));
				writeRdfProperties( subWriter, desc, "\t", usedPrefixes );
				outerWriter.write( "<rdf:Description" );
				writeXmlns( outerWriter, usedPrefixes );
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
	
	public static ParseResult parseRdf( char[] chars, int offset, Map namespaces ) {
		ParseResult xmlParseResult = parseXmlPart(chars, offset);
		Object xmlPart = xmlParseResult.value;
		
		if( xmlPart instanceof XmlOpenTag ) {
			offset = xmlParseResult.newOffset;
			Description desc = new Description();
			
			XmlOpenTag openTag = (XmlOpenTag)xmlPart;
			for( Iterator i=openTag.attributes.keySet().iterator(); i.hasNext(); ) {
				String attrKey = (String)i.next();
				String attrValue = (String)openTag.attributes.get(attrKey);
				if( attrKey.startsWith("xmlns:") ) {
					namespaces.put( attrKey.substring(6), attrValue );
				} else if( "rdf:about".equals(attrKey) ) {
					desc.about = new Ref(attrValue);
				} else {
					// somehow report unrecognised attr?
				}
			}
			if( openTag.closed ) {
				return new ParseResult( desc, offset );
			}
			while( true ) {
				xmlParseResult = parseXmlPart(chars, offset);
				xmlPart = xmlParseResult.value;
				if( xmlPart instanceof XmlCloseTag ) {
					if( !openTag.name.equals(((XmlCloseTag)xmlPart).name) ) {
						throw new RuntimeException("Start and end tags do not match: " + openTag.name + " != " + ((XmlCloseTag)xmlPart).name);
					}
					return new ParseResult( desc, xmlParseResult.newOffset );
				} else if( xmlPart instanceof XmlOpenTag ) {
					XmlOpenTag predicateOpenTag = (XmlOpenTag)xmlPart;
					String predicate = shortToLong( predicateOpenTag.name, namespaces );
					offset = xmlParseResult.newOffset;
					
					String resourceUri = (String)predicateOpenTag.attributes.get("rdf:resource");
					if( resourceUri != null ) {
						desc.put(predicate, new Ref(resourceUri));
					}
					
					if( !predicateOpenTag.closed ) {					
						while( true ) {
							ParseResult rdfValueParseResult = parseRdf(chars, offset, namespaces);
							offset = rdfValueParseResult.newOffset;
							if( rdfValueParseResult.value == null ) {
								break;
							}
							Object value = rdfValueParseResult.value;
							desc.put(predicate, value);
						}
						ParseResult predicateCloseTagParseResult = parseXmlPart(chars, offset);
						if( !(predicateCloseTagParseResult.value instanceof XmlCloseTag) ) {
							throw new RuntimeException("Expected XML close tag but found " + predicateCloseTagParseResult.value.getClass().getName() );
						}
						XmlCloseTag predicateCloseTag = (XmlCloseTag)predicateCloseTagParseResult.value;
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
		Map namespaces = new HashMap();
		ParseResult rdfParseResult = parseRdf( chars, 0, namespaces );
		return rdfParseResult.value;
	}
	
	public static String RDF_ABOUT = "http://www.w3.org/1999/02/22-rdf-syntax-ns#about";
	public static String DC_CREATOR = "http://purl.org/dc/elements/1.1/creator";
	
	public static void main( String[] args ) {
		Description props = new Description();
		props.about = new Ref("http://www.nuke24.net/about");
		props.put(DC_CREATOR, "TOGoS");
		Description subProps = new Description();
		subProps.put("junk/extraprop", "This is an extra property");
		props.put("junk/has-more-props", subProps);
		props.put("junk/part-of-site", new Ref("http://www.nuke24.net/"));
		String rdf = RDF.xmlEncodeRdf(props);
		System.out.println(rdf);
		
		
		Object parsed = rdfToMap(rdf);
		rdf = RDF.xmlEncodeRdf(parsed);
		System.out.println("Parsed: " + rdf);
	}
}
