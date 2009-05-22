package contentcouch.graphics.activefunctions;

import java.awt.image.BufferedImage;
import java.util.Map;

import contentcouch.active.BaseActiveFunction;
import contentcouch.graphics.ImageUtil;
import contentcouch.misc.ValueUtil;

public class SerializeImage extends BaseActiveFunction {
	public Object call( Map argumentExpressions ) {
		BufferedImage img = ImageUtil.getImage(getArgumentValue(argumentExpressions, "operand", null));
		if( img == null ) return null;
		final String formatName = ValueUtil.getString(getArgumentValue(argumentExpressions, "format", "png"));
		final Number quality = ValueUtil.getNumber(getArgumentValue(argumentExpressions, "quality", null));
		
		return ImageUtil.serializeImage(img, formatName, quality);
	}
}
