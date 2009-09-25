package contentcouch.graphics.activefunctions;

import java.awt.image.BufferedImage;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.graphics.ImageUtil;
import contentcouch.misc.ValueUtil;

public class SerializeImage extends BaseActiveFunction {
	public Response call( Request req, Map argumentExpressions ) {
		BufferedImage img = ImageUtil.getImage(getArgumentValue(req, argumentExpressions, "operand", null));
		if( img == null ) return null;
		final String formatName = ValueUtil.getString(getArgumentValue(req, argumentExpressions, "format", "png"));
		final String longFormatName = ImageUtil.getLongFormatName(formatName);
		final Number quality = ValueUtil.getNumber(getArgumentValue(req, argumentExpressions, "quality", null));
		
		return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, ImageUtil.serializeImage(img, formatName, quality), longFormatName);
	}
}
