package contentcouch.graphics.activefunctions;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import togos.mf.api.Request;
import togos.swf2.SwfNamespace;
import contentcouch.active.CachingActiveFunction;
import contentcouch.active.expression.ValueExpression;
import contentcouch.blob.Blob;
import contentcouch.blob.BlobInputStream;
import contentcouch.blob.BlobUtil;
import contentcouch.graphics.ImageUtil;
import contentcouch.misc.MapUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.CCouchNamespace;

public class Thumbnail extends CachingActiveFunction {
	/*
	 * To use ImageMagick to convert images instead of converting internally
	 * (this handles more input formats and results in higher-quality thumbnails)
	 * add these lines to ccouch-config:
	 * 
	 * -config http://ns.nuke24.net/ContentCouch/Config/graphics/thumbnail/method ImageMagick
	 * -config http://ns.nuke24.net/SWF2/Config/ExternalApplications/ImageMagick/convert/exePath C:\apps\imagemagick-6.3.7-Q16\convert.exe
	 * 
	 * (replacing 'C:\apps\imagemagick-6.3.7-Q16\convert.exe' with the path to convert.exe on your computer)
	 */
	
	public static final String CFG_IMCONVERT_EXE = SwfNamespace.CFG_EXTAPPS + "ImageMagick/convert/exePath";
	public static final String CFG_METHOD = CCouchNamespace.CFG_NS + "graphics/thumbnail/method";
	public static final String METHOD_INTERNAL = "Internal";
	public static final String METHOD_IMAGEMAGICK = "ImageMagick";
	
	public String getConvertExe( Request req ) {
		String imConvertExe = (String)MapUtil.getKeyed( req.getMetadata(), CFG_IMCONVERT_EXE, null);
		if( imConvertExe == null ) {
			throw new RuntimeException("Required config var, "+CFG_IMCONVERT_EXE+", not found in config");
		}
		return imConvertExe;
	}
	
	protected Blob getProcessOutput( Process proc ) throws IOException {
		return BlobUtil.readInputStreamIntoBlob(proc.getInputStream(), 8*1024*1024 );
	}
	
	protected Blob getProcessError( Process proc ) throws IOException {
		return BlobUtil.readInputStreamIntoBlob(proc.getErrorStream(), 2048);
	}
	
	protected String joinArgs( String[] args ) {
		String res = "";
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( arg.matches(".*?[ \\*\\<\\>].*") )  {
				arg = "\"" + arg + "\"";
			}
			if( res.length() > 0 ) res += " ";
			res += arg;
		}
		return res;
	}
	
	protected Blob execWithBlobIO( String[] args, Blob input )
		throws IOException
	{
		Process proc = null;
		try {
			proc = Runtime.getRuntime().exec(args);
			if( input != null ) {
				OutputStream  os = proc.getOutputStream();
				BlobUtil.writeBlobToOutputStream(input, os);
				os.close();
			}
			Blob output = getProcessOutput( proc );
			try {
				if( proc.waitFor() != 0 ) {
					String errText;
					try {
						errText = "\n"+ValueUtil.getString(getProcessError(proc));
					} catch( IOException e ) { errText = ""; }
					throw new IOException("Process exited with code "+proc.exitValue()+": " + joinArgs(args) + errText);
				}
			} catch( InterruptedException e ) {
				throw new RuntimeException( e );
			}
			return output;
		} catch( IOException e ) {
			String errText = "";
			if( proc != null ) try {
				errText = "\n"+ValueUtil.getString(getProcessError(proc));
			} catch( IOException e2 ) { }
			throw new RuntimeException("Error while executing "+joinArgs(args)+errText, e );
		}
	}
	
	protected Blob thumbnailifyUsingConvert( Blob input,  int twidth, int theight, Request req ) {
		try {
			String inputFilename;
			Blob inputBlob;
			if( input instanceof File ) {
				inputFilename = ((File)input).getPath();
				inputBlob = null;
			} else {
				inputFilename = "-";
				inputBlob = input;
			}
			return execWithBlobIO( new String[]{getConvertExe(req),inputFilename,"-thumbnail",(twidth+"x"+theight+">"),"-quality","75","jpg:-"}, inputBlob );
		} catch( IOException e ) {
			throw new RuntimeException("IOException while trying to generate thumbnail using 'convert'", e);
		}
	}
	
	protected Blob thumbnailifyInternally( Blob input, int twidth, int theight, Request req ) {
		try {
			BufferedImage img = ImageUtil.getImage(new BlobInputStream(input));
			int newWidth = img.getWidth();
			int newHeight = img.getHeight();
			if( newWidth > twidth ) {
				newHeight = newHeight * twidth / newWidth;
				newWidth = twidth;
			}
			if( newHeight > theight ) {
				newWidth = newWidth * theight / newHeight;
				newHeight = theight;
			}
			BufferedImage thumbnail = ImageUtil.scaleImage(img, newWidth, newHeight);
			return ImageUtil.serializeImage(thumbnail, "jpeg", new Integer(75));
		} catch( IOException e ) {
			throw new RuntimeException("IOException while trying to generate thumbnail internally", e);
		}
	}
	
	protected Blob thumbnailify( Blob input, int twidth, int theight, Request req ) {
		if( METHOD_IMAGEMAGICK.equals(MapUtil.getKeyed(req.getMetadata(), CFG_METHOD, "internal"))) {
			return thumbnailifyUsingConvert( input, twidth, theight, req );
		} else {
			return thumbnailifyInternally( input, twidth, theight, req );
		}
	}
	
	protected String getCacheIndexName( Request req, Map argumentExpressions ) {
		return "thumbnails";
	}
	
	protected Map getCanonicalArgumentExpressions( Map argumentExpressions ) {
		HashMap ae = new HashMap();
		ae.put("operand", argumentExpressions.get("operand"));
		ae.put("width", argumentExpressions.get("width"));
		ae.put("height", argumentExpressions.get("height"));
		if( ae.get("width") == null ) ae.put("width", new ValueExpression("64"));
		if( ae.get("height") == null ) ae.put("height", new ValueExpression("64"));
		return ae;
	}
	
	protected Object _getResult( Request req, Map canonArgExpressions ) {
		int twidth = ValueUtil.getNumber(getArgumentValue(req, canonArgExpressions, "width", "32")).intValue();
		int theight = ValueUtil.getNumber(getArgumentValue(req, canonArgExpressions, "height", "32")).intValue();
		Blob input = (Blob)getArgumentValue(req, canonArgExpressions, "operand", null);
		if( input == null ) throw new RuntimeException("No value for operand");
		try {
			return thumbnailify(input, twidth, theight, req);
		} catch( RuntimeException e ) {
			System.err.println("Error making thumbnail of "+canonArgExpressions.get("operand").toString());
			throw e;
		}
	}
}
