package contentcouch.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
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

import contentcouch.data.Directory;


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
		public String sourceUri;
		public String typeName;
	}
	
	public static class Description extends RdfNode {
		public Ref about;
		public Description() {
			super();
			this.typeName = RDF_DESCRIPTION;
		}
	}
	
	public static class RdfDirectory extends RdfNode implements Directory {
		public static class Entry extends RdfNode implements Directory.Entry {
			public long getLastModified() {
				try {
					return CCOUCH_DATEFORMAT.parse((String)this.getSingle(DC_MODIFIED)).getTime();
				} catch( ParseException e ) {
					throw new RuntimeException(e);
				}
			}
			
			public String getName() {
				return (String)this.getSingle(CCOUCH_NAME);
			}
			
			public Object getTarget() {
				return this.getSingle(CCOUCH_TARGET);
			}
			
			public String getTargetType() {
				return (String)this.getSingle(CCOUCH_TARGETTYPE);
			}
		}
		
		public Map getEntries() {
			List entryList = (List)this.getSingle(CCOUCH_ENTRIES);
			HashMap entries = new HashMap();
			for( Iterator i=entryList.iterator(); i.hasNext(); ) {
				Entry e = (Entry)i.next();
				entries.put(e.getName(), e);
			}
			return entries;
		}
	}

	//// Constants ////

	public static final String URI_PARSE_PREFIX = "x-parse-rdf:";
	
	public static final DateFormat CCOUCH_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String DC_NS  = "http://purl.org/dc/terms/";
	public static final String CCOUCH_NS = "http://ns.nuke24.net/ContentCouch/";
	
	public static final String RDF_ABOUT               = RDF_NS + "about";
	public static final String RDF_RESOURCE            = RDF_NS + "resource";
	public static final String RDF_PARSETYPE           = RDF_NS + "parseType";
	public static final String RDF_DESCRIPTION         = RDF_NS + "Description";
	
	public static final String DC_CREATOR              = DC_NS + "creator";
	public static final String DC_CREATED              = DC_NS + "created";
	public static final String DC_MODIFIED             = DC_NS + "modified";
	public static final String DC_FORMAT               = DC_NS + "format";

	public static final String DC_DESCRIPTION          = CCOUCH_NS + "description";
	public static final String CCOUCH_NAME             = CCOUCH_NS + "name";
	public static final String CCOUCH_TAG              = CCOUCH_NS + "tag";
	public static final String CCOUCH_COLLECTOR        = CCOUCH_NS + "collector";
	public static final String CCOUCH_IMPORTEDDATE     = CCOUCH_NS + "importedDate";
	public static final String CCOUCH_IMPORTEDFROM     = CCOUCH_NS + "importedFrom";
	public static final String CCOUCH_ENTRIES          = CCOUCH_NS + "entries";
	/** What kind of object is target? */
	public static final String CCOUCH_TARGETTYPE       = CCOUCH_NS + "targetType";
	/** What is target? */
	public static final String CCOUCH_TARGET           = CCOUCH_NS + "target";
	/** If we can't directly represent target, link to its listing */
	public static final String CCOUCH_TARGETLISTING    = CCOUCH_NS + "targetListing";
	public static final String CCOUCH_PARENT           = CCOUCH_NS + "parent";
	
	public static final String CCOUCH_DIRECTORY        = CCOUCH_NS + "Directory";
	public static final String CCOUCH_DIRECTORYENTRY   = CCOUCH_NS + "DirectoryEntry";
	public static final String CCOUCH_COMMIT           = CCOUCH_NS + "Commit";
	public static final String CCOUCH_REDIRECT         = CCOUCH_NS + "Redirect";
	
	public static final String OBJECT_TYPE_BLOB = "Blob";
	public static final String OBJECT_TYPE_DIRECTORY = "Directory";
	public static final String OBJECT_TYPE_COMMIT = "Commit";
	/** Indicates a miscellaneous RDF structure */
	public static final String OBJECT_TYPE_RDF = "RDF";
	
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
	
	//// RDF Parsing ////
	
	public static XML.ParseResult parseRdf( char[] chars, int offset, Map nsAbbreviations, String sourceUri ) {
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
			} else if( CCOUCH_DIRECTORY.equals(descOpenTag.name) ) {
				desc = new RdfDirectory();
				desc.typeName = descOpenTag.name;
			} else if( CCOUCH_DIRECTORYENTRY.equals(descOpenTag.name) ) {
				desc = new RdfDirectory.Entry();
				desc.typeName = descOpenTag.name;
			} else {
				 desc = new RdfNode();
				 desc.typeName = descOpenTag.name;
			}
			desc.sourceUri = sourceUri; 
			
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
							XML.ParseResult rdfValueParseResult = parseRdf(chars, offset, predicateNsAbbreviations, sourceUri);
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
	
	public static Object parseRdf( String rdf, String sourceUri ) {
		char[] chars = new char[rdf.length()];
		rdf.getChars(0, chars.length, chars, 0);
		Map nsAbbreviations = standardNsAbbreviations;
		XML.ParseResult rdfParseResult = parseRdf( chars, 0, nsAbbreviations, sourceUri );
		if( rdfParseResult.value instanceof RdfNode ) {
			((RdfNode)rdfParseResult.value).sourceUri = sourceUri;
		}
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
		
		Object parsed = parseRdf(rdf, "file:junk/dir-test.rdf");
		rdf = RDF.xmlEncodeRdf(parsed);
		System.out.println("Output: " + rdf);
	}
}