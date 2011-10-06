package contentcouch.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class JSON {
	
	//// Reading ////
	
	public static class UnexpectedCharacterException
		extends IOException
	{
		private static final long serialVersionUID = 1L;
		
		int got;
		String expected;
		
		public UnexpectedCharacterException(int got, String expected) {
			this.got = got;
			this.expected = expected;
		}
		public UnexpectedCharacterException(int got) {
			this.got = got;
		}
		public String getMessage() {
			String m = "Unexpected char: '" + (char)got + "' (" + got + ")"; 
			if( expected != null ) {
				m += ". Expected " + expected;
			}
			return m;
		}
	}
	
	public static void readMapContent(Reader r, Map m)
		throws IOException
	{
		while(true) {
			skipWhitespace(r);
			r.mark(1);
			int c = r.read();
			r.reset();
			if( c == -1 || c == '}' ) {  return;  }
			
			Object k = readObject(r); 

			skipWhitespace(r);
			r.mark(1);
			c = r.read();
			if( c != ':' ) {
				throw new UnexpectedCharacterException((char) c, "':'");
			}			
			skipWhitespace(r);

			Object v = readObject(r);
			
			m.put(k,v);
			
			skipWhitespace(r);			
			r.mark(1);
			c = r.read();
			if( c != ',' ) {
				r.reset();
			}
		}		
	}
	
	public static void readListContent(Reader r, List l)
		throws IOException
	{
		while(true) {
			skipWhitespace(r);
			r.mark(1);
			int c = r.read();
			r.reset();
			if( c == -1 || c == ']' ) {  return;  }
			
			l.add(readObject(r));
			skipWhitespace(r);
			r.mark(1);
			c = r.read();
			if( c != ',' ) {
				r.reset();
			}
		}
	}
	
	public static void skipWhitespace(Reader r)
		throws IOException
	{
		int c;
		r.mark(1);
		while( (c = r.read()) != -1 &&
			   (c == ' ' || c == '\t' || c == '\r' || c == '\n') ) {
			r.mark(1);
		}
		if( c != -1 ) {  r.reset();  }
	}
	
	public static String readStringContent(Reader r)
		throws IOException
	{
		StringBuffer b = new StringBuffer();
		int c;
		r.mark(1);
		while( (c = r.read()) != -1 && c != '"' ) {
			if( c == '\\' ) {
				c = r.read();
				switch( c ) {
				case('t'): c = '\t'; break;
				case('r'): c = '\r'; break;
				case('n'): c = '\n'; break;
				}
			}
			b.append((char) c);
			r.mark(1);
		}
		if(c != -1 ) {  r.reset();  }
		return b.toString();
	}
	
	public static String readToken(Reader r)
		throws IOException
	{
		String s = "";
		int c;
		r.mark(1);
		while( (c = r.read()) != -1 &&
			   c != ' ' && c != '\t' && c != '\r' && c != '\n' &&
			   c != ',' && c != ':' && c != '}' && c != ']' ) {
			s += (char) c;
			r.mark(1);
		}
		if( c != -1 ) {  r.reset();  }
		return s;
	}
	
	public static Object readObject(Reader r)
		throws IOException
	{
		int c = r.read();
		switch(c) {
		case('+'): case('-'):
		case('0'): case('1'): case('2'): case('3'): case('4'):
		case('5'): case('6'): case('7'): case('8'): case('9'):
			String token = readToken(r);
			return Double.valueOf(((char) c) + token); 
		case('{'):
			Map m = new HashMap();
			readMapContent(r, m);
			skipWhitespace(r);
			c = r.read();
			if( c != '}' ) {
				throw new UnexpectedCharacterException( (char)c, "'}'" );
			}
			return m;
		case('['):
			List l = new ArrayList();
			readListContent(r, l);
			skipWhitespace(r);
			c = r.read();
			if( c != ']' ) {
				throw new UnexpectedCharacterException( (char)c, "']'" );
			}
			return l;
		case('"'):
			String s = readStringContent(r);
			c = r.read();
			if( c != '"' ) {
				throw new UnexpectedCharacterException( (char)c, "'\"'" );
			}
			return s;
		}
		throw new UnexpectedCharacterException((char) c);
	}
	
	//// Writing ////
	
	public static void writeMapContent(Writer w, Map m)
		throws IOException
	{
		Iterator i = m.entrySet().iterator();
		while( i.hasNext() ) {
			Entry e = (Entry)i.next();
			writeObject(w, e.getKey());
			w.write(": ");
			writeObject(w, e.getValue());
			if( i.hasNext() ) {
				w.write(", ");
			}
		}
	}
	
	public static void writeListContent(Writer w, List l)
		throws IOException
	{
		Iterator i = l.iterator();
		while( i.hasNext() ) {
			writeObject(w, i.next());
			if( i.hasNext() ) {
				w.write(", ");
			}
		}
	}
	
	public static void writeObject(Writer w, Object v)
		throws IOException
	{
		if( v == null ) {
			w.write("null");
		} else if( v instanceof Boolean ) {
			if( ((Boolean)v).booleanValue() ) {
				w.write("true");
			} else {
				w.write("false");
			}
		} else if( v instanceof CharSequence ) {
			CharSequence s = (CharSequence) v;
			w.write('"');
			for( int i=0; i<s.length(); ++i ) {
				char c = s.charAt(i);
				switch(c) {
				case('"' ): w.write("\\\""); break;
				case('\\'): w.write("\\\\"); break;
				case('\t'): w.write("\\t"); break;
				case('\r'): w.write("\\r"); break;
				case('\n'): w.write("\\n"); break;
				default: w.write(c);
				}
			}
			w.write('"');
		} else if( v instanceof Number ) {
			w.write( v.toString() );
		} else if( v instanceof Map ) {
			w.write("{");
			writeMapContent(w, (Map) v);
			w.write("}");
		} else if( v instanceof List ) {
			w.write("[");
			writeListContent(w, (List) v);
			w.write("]");
		} else if( v instanceof Object[] ) {
			w.write("[");
			Object[] a = (Object[]) v;
			for( int i=0; i<a.length; ++i ) {
				writeObject(w, a[i]);
				if(i<a.length-1) {
					w.write(", ");
				}
			}
			w.write("]");
		} else if( v instanceof int[] ) {
			w.write("[");
			int[] a = (int[]) v;
			for( int i=0; i<a.length; ++i ) {
				w.write(Integer.toString(a[i]));
				if(i<a.length-1) {
					w.write(", ");
				}
			}
			w.write("]");
		} else if( v instanceof float[] ) {
			w.write("[");
			float[] a = (float[]) v;
			for( int i=0; i<a.length; ++i ) {
				w.write(Float.toString(a[i]));
				if(i<a.length-1) {
					w.write(", ");
				}
			}
			w.write("]");
		} else if( v instanceof double[] ) {
			w.write("[");
			double[] a = (double[]) v;
			for( int i=0; i<a.length; ++i ) {
				w.write(Double.toString(a[i]));
				if(i<a.length-1) {
					w.write(", ");
				}
			}
			w.write("]");
		} else {
			throw new IOException("Don't know how to JSON-ify " + v.toString() );
		}
	}
	
	public static String encodeObject(Object o) {
		StringWriter w = new StringWriter();
		try {
			writeObject(w, o);
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
		return w.toString();
	}

	public static Object decodeObject(String s) {
		try {
			return readObject(new StringReader(s));
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
}
