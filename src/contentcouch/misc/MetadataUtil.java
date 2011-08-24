package contentcouch.misc;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import togos.mf.api.ContentAndMetadata;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import contentcouch.blob.Blob;
import contentcouch.blob.BlobUtil;
import contentcouch.date.DateUtil;
import contentcouch.directory.SimpleDirectory;
import contentcouch.directory.WritableDirectory;
import contentcouch.file.FileBlob;
import contentcouch.framework.TheGetter;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.rdf.DcNamespace;
import contentcouch.value.Ref;

public class MetadataUtil {
	
	//// Content-Type stuff ////
	
	// Assuming all text or HTML served is UTF-8 for now:
	public static String CT_TEXT = "text/plain; charset=utf-8";
	public static String CT_HTML = "text/html; charset=utf-8";
	public static String CT_NFO  = "text/x-nfo";
	public static String CT_JS   = "application/javascript";
	public static String CT_RDF  = "application/rdf+xml";
	public static String CT_SLF  = "application/x-simple-list-file";
	public static String CT_BMP  = "image/bmp";
	public static String CT_PNG  = "image/png";
	public static String CT_GIF  = "image/gif";
	public static String CT_JPEG = "image/jpeg";
	public static String CT_ICO  = "image/vnd.microsoft.icon";
	public static String CT_OGG  = "audio/ogg";
	public static String CT_MP3  = "audio/mpeg";
	public static String CT_FLAC = "audio/flac";
	
	public static HashMap commonTypesByExtension = new HashMap();
	static {
		commonTypesByExtension.put("txt", CT_TEXT);
		commonTypesByExtension.put("html", CT_HTML);
		commonTypesByExtension.put("nfo", CT_NFO);
		commonTypesByExtension.put("js", CT_JS);
		commonTypesByExtension.put("rdf", CT_RDF);
		commonTypesByExtension.put("slf", CT_SLF);
		commonTypesByExtension.put("bmp", CT_BMP);
		commonTypesByExtension.put("gif", CT_GIF);
		commonTypesByExtension.put("png", CT_PNG);
		commonTypesByExtension.put("ico", CT_ICO);
		commonTypesByExtension.put("ogg", CT_OGG);
		commonTypesByExtension.put("mp3", CT_MP3);
		commonTypesByExtension.put("flac", CT_FLAC);
	}
	
	public static boolean looksLikeRdfBlob( Blob b ) {
		if( b.getLength() >= 20 ) {
			byte[] data = b.getData(0, 20);
			try {
				String s = ValueUtil.UTF_8_DECODER.decode(ByteBuffer.wrap(data)).toString();
				return s.startsWith("<RDF") ||
					s.startsWith("<Commit") ||
					s.startsWith("<Directory") ||
					s.startsWith("<Redirect") ||
					s.startsWith("<Description");
			} catch( CharacterCodingException e ) {
			}
		}
		return false;
	}
	

	public static boolean looksLikePlainText( Blob b ) {
		long checkLength = b.getLength();
		if( checkLength == -1 || checkLength > 1024 ) checkLength = 1024;
		
		byte[] data = b.getData(0, (int)checkLength);
		try {
			ValueUtil.UTF_8_DECODER.decode(ByteBuffer.wrap(data)).toString();
			return true;
		} catch( CharacterCodingException e ) {
			return false;
		}
	}
	
	static final Pattern HTMLPATTERN = Pattern.compile(".*<html.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

	public static boolean looksLikeHtml( Blob b ) {
		long checkLength = b.getLength();
		if( checkLength == -1 || checkLength > 1024 ) checkLength = 1024;
		
		byte[] data = b.getData(0, (int)checkLength);
		try {
			String text = ValueUtil.UTF_8_DECODER.decode(ByteBuffer.wrap(data)).toString();
			return HTMLPATTERN.matcher(text).matches();
		} catch( CharacterCodingException e ) {
			return false;
		}
	}
	
	public static String guessContentType( Blob b ) {
		if( b instanceof FileBlob ) {
			String n = ((FileBlob)b).getName();
			if( n.endsWith(".rdf") ) return CT_RDF;
			if( n.endsWith(".html") ) return CT_HTML;
			if( n.endsWith(".slf") ) return CT_SLF;
		}

		if( looksLikeRdfBlob(b)) return CT_RDF;
		if( looksLikeHtml(b)) return CT_HTML;
		if( looksLikePlainText(b)) return CT_TEXT;
		
		if( b.getLength() >= 4 ) {
			byte[] magic = b.getData(0, 4);
			int magicN =
				((magic[0] & 0xFF) << 24) |
				((magic[1] & 0xFF) << 16) |
				((magic[2] & 0xFF) <<  8) |
				((magic[3] & 0xFF) <<  0);
			switch( magicN ) {
			case( 0x47494638 ): return CT_GIF; 
			case( 0x89504E47 ): return CT_PNG;
			}

			switch( (magicN >> 16) & 0xFFFF ) {
			case( 0xFFD8 ): return CT_JPEG;
			case( 0x424D ): return CT_BMP;
			}
		}

		return null;
	}
	
	public static String guessContentTypeByName( String path ) {
		path = path.toLowerCase();
		int ldot = path.lastIndexOf('.');
		// Longest known extensions are 4 chars, so...
		if( ldot != -1 && ldot >= path.length()-5 ) {
			String ext = path.substring(ldot+1);
			return (String)commonTypesByExtension.get(ext);
		}
		return null;
	}
	
	public static String getContentType( ContentAndMetadata res ) {
		String type = (String)res.getContentMetadata().get(DcNamespace.DC_FORMAT);
		if( type != null ) {
			return type;
		}
		if( res.getContent() instanceof Blob ) return guessContentType((Blob)res.getContent());
		return null;
	}
	
	public static String getContentType( ContentAndMetadata res, String path ) {
		String type = (String)res.getContentMetadata().get(DcNamespace.DC_FORMAT);
		if( type != null ) {
			return type;
		}
		type = guessContentTypeByName( path );
		if( type != null ) {
			return type;
		}
		if( res.getContent() instanceof Blob ) {
			type = guessContentType((Blob)res.getContent());
			if( type != null ) {
				return type;
			}
		}
		return null; 
	}
	
	//// Last-modified stuff ////
	
	protected static Date guessLastModified( Blob b ) {
		if( b instanceof FileBlob ) {
			return new Date(((FileBlob)b).lastModified());
		}
		return null;
	}
	
	public static Date getLastModified( ContentAndMetadata res ) {
		Date lastModified = DateUtil.getDate(res.getContentMetadata().get(DcNamespace.DC_MODIFIED));
		if( lastModified != null ) return lastModified;
		if( res.getContent() instanceof Blob ) {
			lastModified = guessLastModified((Blob)res.getContent());
		}
		return lastModified;
	}
	
	////
	
	public static String getStoredIdentifier( Response res ) {
		return ValueUtil.getString(res.getMetadata().get(CCouchNamespace.RES_STORED_IDENTIFIER));
	}
	
	public static void copyStoredIdentifier( Response source, BaseResponse dest, String prefix ) {
		String storedUri = getStoredIdentifier(source);
		if( storedUri != null ) {
			dest.putMetadata( CCouchNamespace.RES_STORED_IDENTIFIER, prefix == null ? storedUri : prefix + storedUri );
		}
	}
	
	public static String getSourceUriOrUnknown( Map metadata ) {
		String sourceUri = (String)metadata.get(CCouchNamespace.SOURCE_URI);
		if( sourceUri == null ) sourceUri = "x-undefined:source";
		return sourceUri;
	}
	
	public static boolean isEntryTrue( Map m, String key, boolean defaultValue ) {
		Object o = m.get(key);
		if( o == null ) return defaultValue;
		if( o == Boolean.FALSE ) return false;
		if( o instanceof Number && ((Number)o).intValue() == 0 ) return false;
		if( o instanceof String && ((String)o).length() == 0 ) return false;
		return true;
	}
	
	public static boolean isEntryTrue( Map m, String key ) {
		return isEntryTrue(m, key, false);
	}
	
	public static void saveCcouchUri( WritableDirectory dir, String dirUri ) {
		SimpleDirectory.Entry uriDotFileEntry = new SimpleDirectory.Entry();
		uriDotFileEntry.target = BlobUtil.getBlob(dirUri);
		uriDotFileEntry.lastModified = new Date().getTime();
		uriDotFileEntry.name = ".ccouch-uri";
		uriDotFileEntry.targetType = CCouchNamespace.TT_SHORTHAND_BLOB;
		uriDotFileEntry.targetSize = ((Blob)uriDotFileEntry.target).getLength();
		((WritableDirectory)dir).addDirectoryEntry(uriDotFileEntry, Collections.EMPTY_MAP);
	}

	public static void dereferenceTargetToRequest( Object target, BaseRequest req ) {
		if( target instanceof Ref ) {
			String targetSourceUri = ((Ref)target).getTargetUri();
			BaseRequest targetReq = new BaseRequest(RequestVerbs.GET, targetSourceUri );
			Response targetRes = TheGetter.call( targetReq );
			req.content = TheGetter.getResponseValueNN( targetRes, targetReq );
			req.contentMetadata = targetRes.getContentMetadata();
			req.putContentMetadata( CCouchNamespace.SOURCE_URI, targetSourceUri );
		} else {
			req.content = target;
		}
	}
}
