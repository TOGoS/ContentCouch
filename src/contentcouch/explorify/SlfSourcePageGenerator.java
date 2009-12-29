/**
 * 
 */
package contentcouch.explorify;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;

import togos.mf.api.Request;
import togos.mf.value.Blob;
import contentcouch.hashcache.SimpleListFile;
import contentcouch.hashcache.SimpleListFile.Chunk;
import contentcouch.misc.ValueUtil;

public class SlfSourcePageGenerator extends PageGenerator {
	protected char[] hexChars = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

	Blob blob;
	String title;
	
	public SlfSourcePageGenerator( Blob b, Request req ) {
		super( req );
		this.blob = b;
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
			String s = ValueUtil.UTF_8_DECODER.decode(ByteBuffer.wrap(b)).toString();
			w.print("<b>");
			w.print(s);
			w.print("</b>");
		} catch( CharacterCodingException e ) {
			writeBytesAsHex(b, w);
		}
	}

	protected void openSpan(String color, PrintWriter w) {
		w.write("<span style=\"background-color:" + color + "\">");
	}
	protected void closeSpan(Writer w) throws IOException {
		w.write("</span>");
	}
	
	protected void writeChunkAsHtml(SimpleListFile slf, Chunk c, PrintWriter w) {
		try {
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
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public void writeContent(PrintWriter w) {
		try {
			SimpleListFile slf = new SimpleListFile(blob, "r");
			Chunk c = slf.getFirstChunk();

			while( c != null ) {
				writeChunkAsHtml(slf, c, w);
				c = slf.getChunk(c.nextOffset);
			}
			slf.close();
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}				
}