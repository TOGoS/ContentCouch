package contentcouch.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class RDF {
	//// Types //// 
	
	public static class Ref {
		public String targetUri;
		public Ref(String targetUri) {
			this.targetUri = targetUri;
		}
	}
	
	public static class MultiMap extends HashMap {
		public MultiMap() {
			super();
		}

		public MultiMap(MultiMap n) {
			super(n);
		}
		
		public void add(Object key, Object value) {
			Set i = (Set)get(key);
			if( i == null ) {
				put(key, i = new HashSet());
			}
			i.add(value);
		}

		public Object getSingle(Object key) {
			Set i = (Set)get(key);
			if( i == null ) return null;
			for( Iterator ii=i.iterator(); ii.hasNext(); ) return ii.next();
			return null;
		}

		public void importValues(Map properties) {
			for( Iterator i = properties.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry entry = (Map.Entry)i.next();
				add(entry.getKey(), entry.getValue());
			}
		}
	}
	
	public static class RdfNode extends MultiMap {
		public String typeName;
	}
	
	public static class Description extends RdfNode {
		public Ref about;
		public Description() {
			super();
			this.typeName = RDF_DESCRIPTION;
		}
	}
	
	//// Constants ////
	
	public static DateFormat CCOUCH_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static String DC_NS  = "http://purl.org/dc/terms/";
	public static String CCOUCH_NS = "http://www.nuke24.net/projects/ContentCouch/ns/";
	
	public static String RDF_ABOUT       = RDF_NS + "about";
	public static String RDF_RESOURCE    = RDF_NS + "resource";
	public static String RDF_PARSETYPE   = RDF_NS + "parseType";
	public static String RDF_DESCRIPTION = RDF_NS + "Description";
	
	public static String DC_CREATOR = DC_NS + "creator";
	public static String DC_CREATED = DC_NS + "created";
	public static String DC_MODIFIED = DC_NS + "modified";
	public static String DC_FORMAT = DC_NS + "format";
	
	public static String CCOUCH_NAME = CCOUCH_NS + "name";
	public static String CCOUCH_TAG  = CCOUCH_NS + "tag";
	public static String CCOUCH_COLLECTOR    = CCOUCH_NS + "collector";
	public static String CCOUCH_IMPORTEDDATE = CCOUCH_NS + "importedDate";
	public static String CCOUCH_IMPORTEDFROM = CCOUCH_NS + "importedFrom";
	public static String CCOUCH_ENTRIES  = CCOUCH_NS + "entries";
	public static String CCOUCH_FILETYPE = CCOUCH_NS + "fileType";
	public static String CCOUCH_CONTENT  = CCOUCH_NS + "content"; // For use when fileType = 'File'
	public static String CCOUCH_LISTING  = CCOUCH_NS + "listing"; // For use when fileType = 'Directory'
	public static String CCOUCH_DIRECTORYLISTING = CCOUCH_NS + "DirectoryListing";
	public static String CCOUCH_DIRECTORYENTRY = CCOUCH_NS + "DirectoryEntry";
	
	static Map standardNsAbbreviations = new HashMap();
	static {
		standardNsAbbreviations.put("rdf", RDF_NS);
		standardNsAbbreviations.put("dc", DC_NS);
		standardNsAbbreviations.put("ccouch", CCOUCH_NS);
		standardNsAbbreviations.put("xmlns", "http://www.w3.org/2000/xmlns/");
	}
	
	//// Functions ////
	
	public static void writeRdfValue( Writer w, Object value, String padding, Map usedNsAbbreviations )
		throws IOException
	{
		if( value instanceof RdfNode ) {
			RdfNode desc = (RdfNode)value;
			String valueNodeName = XML.longToShort(desc.typeName, standardNsAbbreviations, usedNsAbbreviations );
			w.write(padding + "<" + valueNodeName + ">\n");
			writeRdfProperties( w, desc, padding + "\t", usedNsAbbreviations);
			w.write(padding + "</" + valueNodeName + ">\n");
		} else {
			throw new RuntimeException("Don't know how to rdf-ify " + value);
		}
	}
	
	public static void writeRdfProperty( Writer w, String propName, Object value, String padding, Map usedNsAbbreviations )
		throws IOException
	{
		String propNodeName = XML.longToShort(propName, standardNsAbbreviations, usedNsAbbreviations);
		
		if( value == null ) {
			// Then we just skip it!
		} else if( value instanceof Ref ) {
			w.write(padding + "<" + propNodeName + " rdf:resource=\"" + XML.xmlEscapeAttributeValue(((Ref)value).targetUri) + "\"/>\n");
		} else if( value instanceof String ) {
			w.write(padding + "<" + propNodeName + ">" + XML.xmlEscapeText((String)value) + "</" + propNodeName + ">\n");
		} else if( value instanceof Collection ) {
			w.write(padding + "<" + propNodeName + " rdf:parseType=\"Collection\">\n");
			Collection c = (Collection)value;
			for( Iterator i = c.iterator(); i.hasNext(); ) {
				writeRdfValue( w, i.next(), padding + "\t", usedNsAbbreviations );
			}
			w.write(padding + "</" + propNodeName + ">\n");
		} else {
			w.write(padding + "<" + propNodeName + ">\n");
			writeRdfValue( w, value, padding + "\t", usedNsAbbreviations );
			w.write(padding + "</" + propNodeName + ">\n");
		}
	}
	
	public static void writeRdfProperties( Writer w, String propName, Collection values, String padding, Map usednsAbbreviations )
		throws IOException
	{
		for( Iterator i = values.iterator(); i.hasNext(); ) {
			writeRdfProperty( w, propName, i.next(), padding, usednsAbbreviations );
		}
	}
	
	public static List sort(Collection stuff) {
		List sortedStuff = new ArrayList(stuff);
		Collections.sort(sortedStuff);
		return sortedStuff;
	}
	
	public static void writeRdfProperties( Writer w, MultiMap properties, String padding, Map usednsAbbreviations )
		throws IOException
	{
		for( Iterator propIter = sort(properties.keySet()).iterator(); propIter.hasNext(); ) {
			String propName = (String)propIter.next();
			Object value = properties.get(propName);
			
			if( value instanceof Collection ) { // Should always be, unless the RDF.Description was created wrong
				writeRdfProperties( w, propName, (Collection)value, padding, usednsAbbreviations );
			} else {
				throw new RuntimeException("RDF.MultiMap contained non-collection as value for "+propName+": " + value);
			}
		}
	}
	
	public static String xmlEncodeRdf( Object value, String defaultNamespace ) {
		try {
			if( value instanceof RdfNode ) {
				RdfNode desc = (RdfNode)value;

				Writer subWriter = new StringWriter();
				Map usedNsAbbreviations = new HashMap();
				usedNsAbbreviations.put("rdf", standardNsAbbreviations.get("rdf"));
				if( defaultNamespace != null ) {
					usedNsAbbreviations.put("", defaultNamespace);
				}
				writeRdfProperties( subWriter, desc, "\t", usedNsAbbreviations );

				String nodeName = XML.longToShort(desc.typeName, standardNsAbbreviations, usedNsAbbreviations);
				Writer outerWriter = new StringWriter();
				outerWriter.write( "<" + nodeName );
				XML.writeXmlns( outerWriter, usedNsAbbreviations );
				if( desc instanceof Description && ((Description)desc).about != null ) {
					outerWriter.write(" rdf:about=\"" + XML.xmlEscapeAttributeValue(((Description)desc).about.targetUri) + "\"");
				}
				outerWriter.write( ">\n" );
				outerWriter.write( subWriter.toString() );
				outerWriter.write( "</" + nodeName + ">\n" );
				return outerWriter.toString();
			} else {
				return XML.xmlEscapeText(value.toString());
			}
		} catch( IOException e ) {
			throw new RuntimeException( "Error while generating xml", e );
		}
	}
	
	public static String xmlEncodeRdf( Object value ) {
		return xmlEncodeRdf( value, null );
	}
	
	public static XML.ParseResult parseRdf( char[] chars, int offset, Map nsAbbreviations ) {
		XML.ParseResult xmlParseResult = XML.parseXmlPart(chars, offset);
		Object xmlPart = xmlParseResult.value;
		
		if( xmlPart instanceof XML.XmlOpenTag ) {
			offset = xmlParseResult.newOffset;
			RdfNode desc;
			
			XML.XmlOpenTag descOpenTag = (XML.XmlOpenTag)xmlPart;
			Map descNsAbbreviatios = XML.updateNamespaces( descOpenTag, nsAbbreviations );
			descOpenTag = (XML.XmlOpenTag)XML.namespaceXmlPart(descOpenTag, descNsAbbreviatios);
			
			if( RDF_DESCRIPTION.equals(descOpenTag.name) ) {
				 desc = new Description();
				 String about = (String)descOpenTag.attributes.get(RDF_ABOUT);
				 if( about != null ) ((Description)desc).about = new Ref(about);
			} else {
				 desc = new RdfNode();
				 desc.typeName = descOpenTag.name;
			}
			
			if( descOpenTag.closed ) {
				return new XML.ParseResult( desc, offset );
			}
			
			while( true ) {
				xmlParseResult = XML.parseXmlPart(chars, offset);
				xmlPart = xmlParseResult.value;
				Map predicateNsAbbreviations = XML.updateNamespaces(xmlPart, descNsAbbreviatios);
				xmlPart = XML.namespaceXmlPart(xmlPart, predicateNsAbbreviations);
				
				if( xmlPart instanceof XML.XmlCloseTag ) {
					if( !descOpenTag.name.equals(((XML.XmlCloseTag)xmlPart).name) ) {
						throw new RuntimeException("Start and end tags do not match: " + descOpenTag.name + " != " + ((XML.XmlCloseTag)xmlPart).name);
					}
					return new XML.ParseResult( desc, xmlParseResult.newOffset );
				} else if( xmlPart instanceof XML.XmlOpenTag ) {
					XML.XmlOpenTag predicateOpenTag = (XML.XmlOpenTag)xmlPart;
					offset = xmlParseResult.newOffset;
					
					String resourceUri = (String)predicateOpenTag.attributes.get(RDF_RESOURCE);
					if( resourceUri != null ) {
						desc.add(predicateOpenTag.name, new Ref(resourceUri));
					}
					
					if( !predicateOpenTag.closed ) {
						Collection c = null;
						if( "Collection".equals(predicateOpenTag.attributes.get(RDF_PARSETYPE)) ) {
							c = new ArrayList();
							desc.add(predicateOpenTag.name, c);
						}
						while( true ) {
							XML.ParseResult rdfValueParseResult = parseRdf(chars, offset, predicateNsAbbreviations);
							offset = rdfValueParseResult.newOffset;
							if( rdfValueParseResult.value == null ) {
								break;
							}
							Object value = rdfValueParseResult.value;
							if( c != null ) {
								c.add(value);
							} else {
								desc.add(predicateOpenTag.name, value);
							}
						}
						XML.ParseResult predicateCloseTagParseResult = XML.parseXmlPart(chars, offset);
						if( !(predicateCloseTagParseResult.value instanceof XML.XmlCloseTag) ) {
							throw new RuntimeException("Expected XML close tag but found " + predicateCloseTagParseResult.value.getClass().getName() );
						}
						XML.XmlCloseTag predicateCloseTag = (XML.XmlCloseTag)predicateCloseTagParseResult.value;
						predicateCloseTag = (XML.XmlCloseTag)XML.namespaceXmlPart(predicateCloseTag, predicateNsAbbreviations);
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
		} else if( xmlPart instanceof XML.XmlCloseTag ) {
			return new XML.ParseResult( null, offset );
		} else {
			return new XML.ParseResult( xmlPart, xmlParseResult.newOffset );
		}
	}
	
	public static Object parseRdf( String rdf ) {
		char[] chars = new char[rdf.length()];
		rdf.getChars(0, chars.length, chars, 0);
		Map nsAbbreviations = standardNsAbbreviations;
		XML.ParseResult rdfParseResult = parseRdf( chars, 0, nsAbbreviations );
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
		String rdf;
		try {
			rdf = new String(readFile("junk/dir-test.rdf"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		System.out.println("Input: " + rdf);
		
		Object parsed = parseRdf(rdf);
		rdf = RDF.xmlEncodeRdf(parsed);
		System.out.println("Output: " + rdf);
	}
}
