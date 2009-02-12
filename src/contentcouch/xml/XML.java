package contentcouch.xml;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XML {
	public static String xmlEscapeText( String text ) {
		return text.replaceAll("&","&amp").replaceAll("<", "&lt;").replaceAll(">","&gt;");
	}
	
	public static String xmlEscapeName( String text ) {
		return text;
	}

	public static String xmlEscapeAttributeValue( String text ) {
		return text.replaceAll("&","&amp").replaceAll("\"","&quot;").replaceAll("<", "&lt;").replaceAll(">","&gt;");
	}
	
	public static String xmlUnescape(String text) {
		char[] chars = new char[text.length()];
		text.getChars(0, text.length(), chars, 0);
		return (String)parseXmlText(chars, 0, '<').value;
	}

	public static String longToShort( String name, Map availableNsAbbreviations, Map usedNsAbbreviations ) {
		for( Iterator i=usedNsAbbreviations.keySet().iterator(); i.hasNext(); ) {
			String nsShort = (String)i.next();
			String nsLong = (String)usedNsAbbreviations.get(nsShort);
			if( name.length() > nsLong.length() && name.startsWith(nsLong) ) {
				String postfix = name.substring(nsLong.length());
				return nsShort.length() == 0 ? postfix : nsShort + ":" + postfix;
			}
		}
		for( Iterator i=availableNsAbbreviations.keySet().iterator(); i.hasNext(); ) {
			String nsShort = (String)i.next();
			String nsLong = (String)availableNsAbbreviations.get(nsShort);
			if( name.length() > nsLong.length() && name.startsWith(nsLong) ) {
				String postfix = name.substring(nsLong.length());
				usedNsAbbreviations.put(nsShort,nsLong);
				return nsShort.length() == 0 ? postfix : nsShort + ":" + postfix;
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
	
	public static void writeXmlns( Writer w, Map nsAbbreviations )
	throws IOException
	{
		ArrayList keys = new ArrayList(nsAbbreviations.keySet());
		Collections.sort(keys);
		for( Iterator i=keys.iterator(); i.hasNext(); ) {
			String nsShort = (String)i.next();
			String nsLong = (String)nsAbbreviations.get(nsShort);
			w.write((nsShort.length() == 0 ? " xmlns" : " xmlns:" + nsShort) + "=\"" + xmlEscapeAttributeValue(nsLong) + "\"");
		}
	}

	public final static class ParseResult {
		public Object value;
		public int newOffset;
		public ParseResult( Object value, int newOffset ) {
			this.value = value;
			this.newOffset = newOffset;
		}
	}
	
	public final static class XmlAttribute {
		public String name;
		public String value;
		public XmlAttribute( String name, String value ) {
			this.name = name;
			this.value = value;
		}
	}
	
	public final static class XmlCloseTag {
		public String name;
		public XmlCloseTag(String name) {
			this.name = name;
		}
	}
	
	public final static class XmlOpenTag {
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
		StringBuffer nameBuilder = new StringBuffer();
		
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
}
