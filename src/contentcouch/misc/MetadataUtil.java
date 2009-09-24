package contentcouch.misc;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import togos.mf.api.ContentAndMetadata;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import togos.mf.value.Blob;
import contentcouch.blob.BlobUtil;
import contentcouch.date.DateUtil;
import contentcouch.directory.WritableDirectory;
import contentcouch.file.FileBlob;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.rdf.DcNamespace;
import contentcouch.store.TheGetter;
import contentcouch.value.Ref;

public class MetadataUtil {
	
	//// Content-Type stuff ////
	
	public static String CT_RDF  = "application/rdf+xml";
	public static String CT_SLF  = "application/x-simple-list-file";
	public static String CT_TEXT = "text/plain";
	public static String CT_HTML = "text/html";
	public static String CT_BMP  = "image/bmp";
	public static String CT_PNG  = "image/png";
	public static String CT_GIF  = "image/gif";
	public static String CT_JPEG = "image/jpeg";
	public static String CT_OGG  = "audio/ogg";
	public static String CT_MP3  = "audio/mpeg";
	
	public static HashMap commonTypesByExtension = new HashMap();
	static {
		commonTypesByExtension.put("rdf", CT_RDF);
		commonTypesByExtension.put("slf", CT_SLF);
		commonTypesByExtension.put("txt", CT_TEXT);
		commonTypesByExtension.put("html", CT_HTML);
		commonTypesByExtension.put("bmp", CT_BMP);
		commonTypesByExtension.put("gif", CT_GIF);
		commonTypesByExtension.put("png", CT_PNG);
		commonTypesByExtension.put("ogg", CT_OGG);
		commonTypesByExtension.put("mp3", CT_MP3);
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
		if( b.getLength() >= 20 ) {
			byte[] data = b.getData(0, 20);
			try {
				ValueUtil.UTF_8_DECODER.decode(ByteBuffer.wrap(data)).toString();
				return true;
			} catch( CharacterCodingException e ) {
			}
		}
		return false;
	}
	
	static final Pattern HTMLPATTERN = Pattern.compile(".*<html.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

	public static boolean looksLikeHtml( Blob b ) {
		if( b.getLength() >= 20 ) {
			byte[] data = b.getData(0, 20);
			try {
				String text = ValueUtil.UTF_8_DECODER.decode(ByteBuffer.wrap(data)).toString();
				return HTMLPATTERN.matcher(text).matches();
			} catch( CharacterCodingException e ) {
			}
		}
		return false;
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
			case( 0x89504E47 ): return CT_PNG;
			}

			switch( (magicN >> 16) & 0xFFFF ) {
			case( 0xFFD8 ): return CT_JPEG;
			case( 0x424D ): return CT_BMP;
			}
		}

		return null;
	}
	
	public static String getContentType( ContentAndMetadata res ) {
		String type = (String)res.getContentMetadata().get(DcNamespace.DC_FORMAT);
		if( type != null ) return type.split(";")[0];
		if( res.getContent() instanceof Blob ) return guessContentType((Blob)res.getContent());
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
		return ValueUtil.getString(res.getMetadata().get(CcouchNamespace.RES_STORED_IDENTIFIER));
	}
	
	public static void copyStoredIdentifier( Response source, BaseResponse dest, String prefix ) {
		String storedUri = getStoredIdentifier(source);
		if( storedUri != null ) {
			dest.putMetadata( CcouchNamespace.RES_STORED_IDENTIFIER, prefix == null ? storedUri : prefix + storedUri );
		}
	}
	
	public static String getSourceUriOrUnknown( Map metadata ) {
		String sourceUri = (String)metadata.get(CcouchNamespace.SOURCE_URI);
		if( sourceUri == null ) sourceUri = "x-undefined:source";
		return sourceUri;
	}
	
	public static boolean isEntryTrue( Map m, String key ) {
		Object o = m.get(key);
		if( o == null ) return false;
		if( o == Boolean.FALSE ) return false;
		if( o instanceof Number && ((Number)o).intValue() == 0 ) return false;
		if( o instanceof String && ((String)o).length() == 0 ) return false;
		return true;
	}
	
	public static void saveCcouchUri( WritableDirectory dir, String dirUri ) {
		SimpleDirectory.Entry uriDotFileEntry = new SimpleDirectory.Entry();
		uriDotFileEntry.target = BlobUtil.getBlob(dirUri);
		uriDotFileEntry.targetLastModified = new Date().getTime();
		uriDotFileEntry.name = ".ccouch-uri";
		uriDotFileEntry.targetType = CcouchNamespace.OBJECT_TYPE_BLOB;
		uriDotFileEntry.targetSize = ((Blob)uriDotFileEntry.target).getLength();
		((WritableDirectory)dir).addDirectoryEntry(uriDotFileEntry);
	}

	public static void dereferenceTargetToRequest( Object target, BaseRequest req ) {
		if( target instanceof Ref ) {
			String targetSourceUri = ((Ref)target).getTargetUri();
			BaseRequest targetReq = new BaseRequest(RequestVerbs.VERB_GET, targetSourceUri );
			Response targetRes = TheGetter.call( targetReq );
			req.content = TheGetter.getResponseValue( targetRes, targetReq );
			req.contentMetadata = targetRes.getContentMetadata();
			req.putContentMetadata( CcouchNamespace.SOURCE_URI, targetSourceUri );
		} else {
			req.content = target;
		}
	}
}
