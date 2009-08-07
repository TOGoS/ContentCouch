package contentcouch.graphics.activefunctions;

import java.awt.image.BufferedImage;
import java.util.Map;

import togos.mf.ResponseCodes;
import togos.mf.Response;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.graphics.ImageUtil;
import contentcouch.misc.ValueUtil;

public class SerializeImage extends BaseActiveFunction {
	public Response call( Map argumentExpressions ) {
		BufferedImage img = ImageUtil.getImage(getArgumentValue(argumentExpressions, "operand", null));
		if( img == null ) return null;
		final String formatName = ValueUtil.getString(getArgumentValue(argumentExpressions, "format", "png"));
		final String longFormatName = ImageUtil.getLongFormatName(formatName);
		final Number quality = ValueUtil.getNumber(getArgumentValue(argumentExpressions, "quality", null));
		
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, ImageUtil.serializeImage(img, formatName, quality), longFormatName);
	}
}
