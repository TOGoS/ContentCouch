package contentcouch.framework.err;

import togos.mf.api.Request;
import togos.mf.api.Response;

public class CallerErrorException extends AbnormalResponseException
{
	public CallerErrorException( Response res, Request req ) { super( res, req ); }
}
