package contentcouch.graphics.activefunctions;

import java.awt.image.BufferedImage;
import java.util.Map;

import contentcouch.active.BaseActiveFunction;
import contentcouch.graphics.ImageUtil;
import contentcouch.misc.ValueUtil;

public class ScaleImage extends BaseActiveFunction {

	/** Possible arguments:
	 * scale  - scale by given amount
	 * scalex - scale this much x-wise
	 * scaley - scale this much y-wise
	 * width  - give new width
	 * height - give new height
	 */
	
	public Object call( Map context, Map argumentExpressions ) {
		BufferedImage img = ImageUtil.getImage(getArgumentValue(context, argumentExpressions, "operand", null));
		if( img == null ) return null;

		int oldWidth = img.getWidth();
		int oldHeight = img.getHeight();
		
		Number scale = ValueUtil.getNumber(getArgumentValue(context, argumentExpressions, "scale", null));
		Number scaleX = ValueUtil.getNumber(getArgumentValue(context, argumentExpressions, "scalex", null));
		Number scaleY = ValueUtil.getNumber(getArgumentValue(context, argumentExpressions, "scaley", null));
		Number width = ValueUtil.getNumber(getArgumentValue(context, argumentExpressions, "width", null));
		Number height = ValueUtil.getNumber(getArgumentValue(context, argumentExpressions, "height", null));
		
		int newWidth = oldWidth;
		int newHeight = oldHeight;
		if( scale != null ) {
			newWidth = (int)(oldWidth * scale.doubleValue());
			newHeight = (int)(oldHeight * scale.doubleValue());
		}
		if( scaleX != null ) newWidth = (int)(oldWidth * scaleX.doubleValue());
		if( scaleY != null ) newHeight = (int)(oldHeight * scaleY.doubleValue());
		if( width != null ) newWidth = width.intValue();
		if( height != null ) newHeight = height.intValue();
		
		return ImageUtil.scaleImage( img, newWidth, newHeight );
	}

}
