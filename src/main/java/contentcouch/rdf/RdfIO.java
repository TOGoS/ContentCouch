package contentcouch.rdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import contentcouch.context.Config;
import contentcouch.value.BaseRef;
import contentcouch.value.Ref;
import contentcouch.xml.XML;

public class RdfIO {
	public enum XMLEncodingContext {
		PIECE,
		FILE
	};
	
	public static void writeRdfValue( Writer w, Object value, String padding, Map usedNsAbbreviations )
		throws IOException
	{
		if( value instanceof RdfNode ) {
			RdfNode desc = (RdfNode)value;
			String valueNodeName = XML.longToShort(desc.getRdfTypeUri(), CCouchNamespace.standardNsAbbreviations, usedNsAbbreviations );
			int wpCount = 0;
			for( Iterator propIter = desc.keySet().iterator(); propIter.hasNext(); ) {
				String propName = (String)propIter.next();
				if( !RdfNamespace.RDF_TYPE.equals(propName) ) ++wpCount;
			}
			String subUri = desc.getSubjectUri();
			String subUriAttrStr;
			if( subUri != null ) {
				subUriAttrStr = " "+XML.longToShort(RdfNamespace.RDF_ABOUT, usedNsAbbreviations, usedNsAbbreviations)+"=\""+XML.xmlEscapeAttributeValue(subUri)+"\"";
			} else {
				subUriAttrStr = "";
			}
			if( wpCount > 0 ) {
				w.write(padding + "<" + valueNodeName + subUriAttrStr + ">\n");
				writeRdfProperties( w, desc, padding + "\t", usedNsAbbreviations);
				w.write(padding + "</" + valueNodeName + ">");
			} else {
				w.write(padding + "<" + valueNodeName + subUriAttrStr + "/>");
			}
		} else {
			throw new RuntimeException("Don't know how to rdf-ify " + value);
		}
	}
	
	/** write a single property, not including trailing newline */
	public static void writeRdfProperty( Writer w, String propName, Object value, String padding, Map usedNsAbbreviations )
		throws IOException
	{
		String propNodeName = XML.longToShort(propName, CCouchNamespace.standardNsAbbreviations, usedNsAbbreviations);
		
		if( value == null ) {
			// Then we just skip it!
		} else if( value instanceof Ref ) {
			w.write(padding + "<" + propNodeName + " rdf:resource=\"" + XML.xmlEscapeAttributeValue(((Ref)value).getTargetUri()) + "\"/>");
		} else if( value instanceof String ) {
			w.write(padding + "<" + propNodeName + ">" + XML.xmlEscapeText((String)value) + "</" + propNodeName + ">");
		} else if( value instanceof Collection ) {
			w.write(padding + "<" + propNodeName + " rdf:parseType=\"Collection\"");
			Collection c = (Collection)value;
			// In old style, empty list -> \t<List rdf:parseType="Collection">\n\t</List>
			// In new style, empty list -> \t<List rdf:parseType="Collection"/>
			if( c.size() > 0 || Config.getRdfDirectoryStyle() == 1 ) {
				w.write(">\n");
				for( Iterator i = c.iterator(); i.hasNext(); ) {
					writeRdfValue( w, i.next(), padding + "\t", usedNsAbbreviations );
					w.write("\n");
				}
				w.write(padding + "</" + propNodeName + ">");
			} else {
				w.write("/>");
			}
		} else {
			w.write(padding + "<" + propNodeName + ">\n");
			writeRdfValue( w, value, padding + "\t", usedNsAbbreviations );
			w.write("\n" + padding + "</" + propNodeName + ">");
		}
	}
	
	/** write all properties.  trailing newline included */
	public static void writeRdfProperties( Writer w, String propName, Collection values, String padding, Map usednsAbbreviations )
		throws IOException
	{
		for( Iterator i = values.iterator(); i.hasNext(); ) {
			writeRdfProperty( w, propName, i.next(), padding, usednsAbbreviations );
			w.write("\n");
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
			if( RdfNamespace.RDF_TYPE.equals(propName) ) continue;
			Object value = properties.get(propName);
			
			if( value instanceof Collection ) { // Should always be, unless the RDF.Description was created wrong
				writeRdfProperties( w, propName, (Collection)value, padding, usednsAbbreviations );
			} else {
				throw new RuntimeException("RDF.MultiMap contained non-collection as value for "+propName+": " + value);
			}
		}
	}
	
	public static String xmlEncodeRdf( Object value, String defaultNamespace, XMLEncodingContext encodingContext ) {
		try {
			Writer outerWriter = new StringWriter();
			if( value instanceof RdfNode ) {
				RdfNode desc = (RdfNode)value;
	
				Writer subWriter = new StringWriter();
				Map usedNsAbbreviations = new HashMap();
				usedNsAbbreviations.put("rdf", CCouchNamespace.standardNsAbbreviations.get("rdf"));
				if( defaultNamespace != null ) {
					usedNsAbbreviations.put("", defaultNamespace);
				}
				writeRdfProperties( subWriter, desc, "\t", usedNsAbbreviations );
				
				String typeUri = desc.getRdfTypeUri();
				if( typeUri == null ) typeUri = RdfNamespace.RDF_DESCRIPTION;
				String nodeName = XML.longToShort(typeUri, CCouchNamespace.standardNsAbbreviations, usedNsAbbreviations);
				
				outerWriter.write( "<" + nodeName );
				XML.writeXmlns( outerWriter, usedNsAbbreviations );
				if( desc.getSubjectUri() != null ) {
					outerWriter.write(" rdf:about=\"" + XML.xmlEscapeAttributeValue(desc.getSubjectUri()) + "\"");
				}
				String nodely = subWriter.toString();
				if( nodely.length() > 0 ) {
					outerWriter.write( ">\n" );
					outerWriter.write( nodely );
					outerWriter.write( "</" + nodeName + ">" );
				} else {
					outerWriter.write( "/>" );
				}
			} else {
				outerWriter.write(XML.xmlEscapeText(value.toString()));
			}
			switch( encodingContext ) {
			case FILE : outerWriter.write("\n"); break;
			case PIECE: break;
			}
			return outerWriter.toString();
		} catch( IOException e ) {
			throw new RuntimeException( "Error while generating xml", e );
		}
	}
	
	public static String xmlEncodeRdf( Object value, XMLEncodingContext encodingContext ) {
		return xmlEncodeRdf( value, null, encodingContext );
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

			// TODO:
			// Always return regular RdfNodes
			// Subject should not _be_ the RDF node, but gotten by
			// Calling RdfInterpreter#interpretSubject
			if( RdfNamespace.RDF_DESCRIPTION.equals(descOpenTag.name) ) {
				 desc = new RdfNode();
			} else if( CCouchNamespace.DIRECTORY.equals(descOpenTag.name) ) {
				desc = new RdfDirectory();
				desc.setRdfTypeUri( descOpenTag.name );
			} else if( CCouchNamespace.DIRECTORYENTRY.equals(descOpenTag.name) ) {
				desc = new RdfDirectory.Entry();
				desc.setRdfTypeUri( descOpenTag.name );
			} else if( CCouchNamespace.COMMIT.equals(descOpenTag.name) ) {
				desc = new RdfCommit();
				desc.setRdfTypeUri( descOpenTag.name );
			} else {
				 desc = new RdfNode(descOpenTag.name);
			}
			desc.subjectUri = (String)descOpenTag.attributes.get(RdfNamespace.RDF_ABOUT);
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
					
					String resourceUri = (String)predicateOpenTag.attributes.get(RdfNamespace.RDF_RESOURCE);
					if( resourceUri != null ) {
						desc.add(predicateOpenTag.name, new BaseRef(resourceUri));
					}
					
					if( !predicateOpenTag.closed ) {
						Collection c = null;
						if( "Collection".equals(predicateOpenTag.attributes.get(RdfNamespace.RDF_PARSETYPE)) ) {
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
		Map nsAbbreviations = CCouchNamespace.standardNsAbbreviations;
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
		try {
			int read = 0;
			int length = (int)file.length();
			byte[] content = new byte[length];
			while( read < length ) {
				read += fis.read(content, read, length-read );
			}
			return content;
		} finally {
			fis.close();
		}
	}	
}
