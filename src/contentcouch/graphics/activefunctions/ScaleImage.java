package contentcouch.graphics.activefunctions;

import java.awt.image.BufferedImage;
import java.util.Map;

import contentcouch.active.BaseActiveFunction;
import contentcouch.graphics.ImageUtil;
import contentcouch.misc.ValueUtil;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;

public class ScaleImage extends BaseActiveFunction {

	/** Possible arguments:
	 * scale  - scale by given amount
	 * scalex - scale this much x-wise
	 * scaley - scale this much y-wise
	 * width  - give new width
	 * height - give new height
	 */
	
	public Response call( Request req, Map argumentExpressions ) {
		BufferedImage img = ImageUtil.getImage(getArgumentValue(req, argumentExpressions, "operand", null));
		if( img == null ) return null;

		int oldWidth = img.getWidth();
		int oldHeight = img.getHeight();
		
		Number scale = ValueUtil.getNumber(getArgumentValue(req, argumentExpressions, "scale", null));
		Number scaleX = ValueUtil.getNumber(getArgumentValue(req, argumentExpressions, "scalex", null));
		Number scaleY = ValueUtil.getNumber(getArgumentValue(req, argumentExpressions, "scaley", null));
		Number width = ValueUtil.getNumber(getArgumentValue(req, argumentExpressions, "width", null));
		Number height = ValueUtil.getNumber(getArgumentValue(req, argumentExpressions, "height", null));
		Number maxWidth = ValueUtil.getNumber(getArgumentValue(req, argumentExpressions, "max-width", null));
		Number maxHeight = ValueUtil.getNumber(getArgumentValue(req, argumentExpressions, "max-height", null));
		boolean preserveAspectRatio = ValueUtil.getBoolean(getArgumentValue(req, argumentExpressions, "preserve-aspect-ratio", null), true);
		
		int newWidth = -1;
		int newHeight = -1;
		if( scale != null ) {
			newWidth = (int)(oldWidth * scale.doubleValue());
			newHeight = (int)(oldHeight * scale.doubleValue());
		}
		if( scaleX != null ) newWidth = (int)(oldWidth * scaleX.doubleValue());
		if( scaleY != null ) newHeight = (int)(oldHeight * scaleY.doubleValue());
		if( width != null ) newWidth = width.intValue();
		if( height != null ) newHeight = height.intValue();
		if( newWidth == -1 && newHeight == -1 ) {
			newWidth = oldWidth;
			newHeight = oldHeight;
		} else if( newWidth == -1 ) {
			newWidth = (int)(preserveAspectRatio ? oldWidth * ((float)newHeight/oldHeight) : oldWidth);
		} else if( newHeight == -1 ) {
			newHeight = (int)(preserveAspectRatio ? oldHeight * ((float)newWidth/oldWidth) : oldWidth);
		}
		
		if( maxWidth != null && newWidth > maxWidth.intValue() ) {
			if( preserveAspectRatio ) newHeight *= (maxWidth.floatValue() / newWidth);
			newWidth = maxWidth.intValue();
		}
		if( maxHeight != null && newHeight > maxHeight.intValue() ) {
			if( preserveAspectRatio ) newWidth *= (maxHeight.floatValue() / newHeight);
			newHeight = maxHeight.intValue();
		}
		
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, ImageUtil.scaleImage( img, newWidth, newHeight ));
	}

}
