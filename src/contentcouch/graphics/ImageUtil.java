package contentcouch.graphics;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import contentcouch.blob.BlobUtil;
import contentcouch.blob.ByteArrayBlob;
import contentcouch.rdf.RdfNamespace;
import contentcouch.value.Blob;

public class ImageUtil {
	
	public static BufferedImage getImage( InputStream inputStream )
		throws IOException
	{
		return ImageIO.read(inputStream);
	}
	
	public static BufferedImage getImage( Object o ) {
		if( o == null ) {
			return null;
		} else if( o instanceof Blob ) {
			try {
				return getImage( BlobUtil.getInputStream((Blob)o) );
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
		} else if( o instanceof byte[] ) {
			try {
				return getImage( new ByteArrayInputStream((byte[])o) );
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
		} else if( o instanceof BufferedImage ) {
			return (BufferedImage)o;
		} else {
			throw new RuntimeException("Can't convert " + o.getClass().getName() + " to Image");
		}
	}
	
	public static Blob serializeImage( final BufferedImage img, final String formatName ) {
		final String longFormatName;
		final String shortFormatName;
		if( "image/png".equals(formatName) || "png".equals(formatName)) {
			shortFormatName = "png";
			longFormatName = "image/png";
		} else if( "image/jpeg".equals(formatName) || "jpg".equals(formatName) || "jpeg".equals(formatName) ) {
			shortFormatName = "jpeg";
			longFormatName = "image/jpeg";
		} else if( formatName.indexOf('/') == -1 ) {
			shortFormatName = formatName;
			longFormatName = "image/" + formatName;
		} else {
			shortFormatName = formatName.substring(formatName.indexOf('/')+1);
			longFormatName = formatName;
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		try {
			ImageIO.write(img, shortFormatName, baos);
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
		
		ByteArrayBlob bab = new ByteArrayBlob(baos.toByteArray());
		bab.putMetadata(RdfNamespace.DC_FORMAT, longFormatName);
		return bab;
	}
	
	public static BufferedImage scaleImage( final BufferedImage img, final int newWidth, final int newHeight ) {
		if( Math.abs(newWidth*newHeight) > 1024*1024*8 ) {
			throw new RuntimeException("Output image would be too large: " + Math.abs(newWidth*newHeight) + " pixels");
		}
		
		BufferedImage bdest = new BufferedImage(Math.abs(newWidth), Math.abs(newHeight), img.getType());
		Graphics2D g = bdest.createGraphics();
		int dx1 = newWidth  < 0 ? -newWidth  : 0;
		int dy1 = newHeight < 0 ? -newHeight : 0;
		int dx2 = newWidth  < 0 ? 0 : newWidth ;
		int dy2 = newHeight < 0 ? 0 : newHeight;
		g.drawImage(img, dx1, dy1, dx2, dy2, 0, 0, img.getWidth(), img.getHeight(), null);
		return bdest;
	}
}
