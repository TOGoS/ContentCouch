package contentcouch.graphics.activefunctions;

import java.awt.image.BufferedImage;
import java.util.Map;

import contentcouch.active.BaseActiveFunction;
import contentcouch.graphics.ImageUtil;
import contentcouch.misc.ValueUtil;

public class SerializeImage extends BaseActiveFunction {
	public Object call(Map context, Map argumentExpressions) {
		BufferedImage img = ImageUtil.getImage(getArgumentValue(context, argumentExpressions, "operand", null));
		if( img == null ) return null;
		final String formatName = ValueUtil.getString(getArgumentValue(context, argumentExpressions, "format", "png"));
		
		return ImageUtil.serializeImage(img, formatName);
	}
}
