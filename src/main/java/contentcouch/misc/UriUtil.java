package contentcouch.misc;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import contentcouch.blob.Blob;
import contentcouch.rdf.CCouchNamespace;

public class UriUtil {
	public static final char[] HEXCHARS = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	protected static final boolean bac( byte[] arr, byte b ) {
		for( int i=arr.length-1; i>=0; --i ) if(arr[i] == b) return true;
		return false;
	}
	protected static byte[] join( byte[] x, byte[] y ) {
		byte[] r = new byte[x.length + y.length];
		for( int i=0; i<x.length; ++i ) r[i] = x[i];
		for( int i=0; i<y.length; ++i ) r[i+x.length] = y[i];
		return r;
	}
	protected static byte[] filter( byte[] arr, byte[] remove ) {
		int length = arr.length;
		for( int i=0; i<arr.length; ++i ) {
			if( bac(remove,arr[i]) ) --length;
		}
		byte[] res = new byte[length];
		int j=0;
		for( int i=0; i<arr.length; ++i ) {
			if( !bac(remove,arr[i]) ) res[j++] = arr[i];
		}
		return res;
	}
	
	protected static byte[] EMPTY = new byte[0];
	
	/*
	 * Based on info from
	 * http://labs.apache.org/webarch/uri/rfc/rfc3986.html#characters
	 */

	/** Non-alphanumeric characters that do not normally have special
	 * meaning in URIs but that are normally escaped anyway */
	public static byte[] UNRESERVED_CHARS = new byte[] {
		'-','.','_','~'
	};
	public static byte[] GEN_DELIMS = new byte[] {
		':','/','?','#','[',']','@'
	};
	public static byte[] SUB_DELIMS = new byte[] {
		'!','$','&','\'','(',')','*','+',',',';','='
	};
	
	/** It is generally safe to include these unescaped within a path component */
	public static byte[] PATH_SEGMENT_SAFE = join(
		UNRESERVED_CHARS,
		new byte[] { ':','!','$','\'','(',')','*',',' }
	);

	public static byte[] PATH_SAFE = join(
		PATH_SEGMENT_SAFE,
		new byte[] { '/' }
	);
	
	/** Characters that have special meaning in URIs */
	public static byte[] RESERVED_CHARS = join(GEN_DELIMS,SUB_DELIMS);

	/** Non-alphanumeric characters that are valid in URIs */
	public static byte[] VALID_CHARS = join(new byte[]{'%'},join(RESERVED_CHARS,UNRESERVED_CHARS));
	/** Valid URI characters that don't have special meaning to pseudo-active syntax.
	 * This is actually a dumb way to do things, because you might not get what you
	 * expect when you decode if these chars have special meaning in the encoded URI... :P */
	public static byte[] VALID_CHARS_NOFUNC = filter(VALID_CHARS, new byte[]{'(',')','[',']','*'}); 
	
	public static String uriEncode( byte[] inbytes, byte[] doNotEscape ) {
		char[] outchars = new char[inbytes.length*3];
		int inidx=0, outidx=0;
		while( inidx < inbytes.length ) {
			byte c = inbytes[inidx++];
			if( (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
			    (c >= '0' && c <= '9') || bac(doNotEscape,c) )
			{
				outchars[outidx++] = (char)c;
			} else {
				outchars[outidx++] = '%';
				outchars[outidx++] = HEXCHARS[(c>>4)&0xF];
				outchars[outidx++] = HEXCHARS[ c    &0xF];
			}
		}
		return new String(outchars,0,outidx);
	}
	
	public static String uriEncode( byte[] inbytes ) {
		return uriEncode( inbytes, EMPTY );
	}
	
	public static String uriEncode( String text, byte[] doNotEncode ) {
		if( text == null ) return null;
		byte[] inbytes;
		try {
			inbytes = text.getBytes("UTF-8");
		} catch( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
		return uriEncode( inbytes, doNotEncode );
	}
	
	public static String uriEncode( String text ) {
		return uriEncode( text, UNRESERVED_CHARS );
	}

	/** Ensures that all characters are valid URI characters.
	 * Does not encode valid characters. */
	public static String sanitizeUri( String text ) {
		return uriEncode( text, VALID_CHARS );
	}

	protected static final int hexValue( byte digit ) {
		return digit <= '9' ? digit - '0' : digit <= 'F' ? digit - 'A' + 10 : digit - 'a' + 10;
	}
	
	protected static final int hexValue( byte hiDigit, byte loDigit ) {
		return (hexValue(hiDigit) << 4) | hexValue(loDigit);
	}
	
	public static byte[] uriDecodeBytes( byte[] text ) {
		int escapecount = 0;
		for( int i=text.length-1; i>=0; --i ) {
			if( text[i] == '%' ) ++escapecount;
		}
		byte[] outbytes = new byte[text.length - (escapecount<<1)];
		int inidx=0, outidx=0;
		while( inidx < text.length ) {
			byte c = text[inidx++];
			if( c == '%' ) {
				byte hiDigit = text[inidx++];
				byte loDigit = text[inidx++];
				outbytes[outidx++] = (byte)hexValue(hiDigit, loDigit);
			} else {
				outbytes[outidx++] = (byte)c;
			}
		}
		return outbytes;
	}
	
	public static byte[] uriDecodeBytes( String text ) {
		return uriDecodeBytes( ValueUtil.getBytes(text) );
	}
	
	public static String uriDecode( String text ) {
		try {
			return new String(uriDecodeBytes(text), "UTF-8");
		} catch( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public static String makeDataUri( byte[] data ) {
		return "data:," + uriEncode(data);
	}

	public static String makeDataUri( String data ) {
		try {
			return makeDataUri(data.getBytes("UTF-8"));
		} catch( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public static String makeDataUri( Blob b ) {
		return makeDataUri( b.getData(0, (int)b.getLength()));
	}
	
	/**
	 * @param uri
	 * @return the part of the URI after x-parse-rdf: or x-rdf-subject:, or null
	 *   if the URI does not start with one of those prefixes. 
	 */
	public static final String stripRdfSubjectPrefix( String uri ) {
		for( int i=CCouchNamespace.RDF_SUBJECT_URI_PREFIXES.length-1; i>=0; --i ) {
			String pfx = CCouchNamespace.RDF_SUBJECT_URI_PREFIXES[i];
			if( uri.startsWith(pfx) ) {
				return uri.substring(pfx.length());
			}
		}
		return null;
	}
	
	static Pattern CONTENT_URI_PATTERN = Pattern.compile("^(?:x-rdf-subject:|x-parse-rdf:)?urn:(?:tree:)?(?:sha1:|tiger:|bitprint:)(.+)$");
	
	/**
	 * @return true iff the URI is of a supported scheme that:
	 *   * identifies an object based on its inherent content
	 *   * can be used to verify an object once fetched
	 *   * does NOT encode location information
	 */
	public static boolean isPureContentUri( String uri ) {
		return CONTENT_URI_PATTERN.matcher(uri).matches();
	}
}
