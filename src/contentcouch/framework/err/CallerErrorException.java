package contentcouch.framework.err;

import togos.mf.api.Request;
import togos.mf.api.Response;

public class CallerErrorException extends AbnormalResponseException
{
	private static final long serialVersionUID = 1L;
	
	public CallerErrorException( Response res, Request req ) { super( res, req ); }
}
