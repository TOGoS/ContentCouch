package contentcouch.framework.err;

import togos.mf.api.Request;
import togos.mf.api.Response;

public class DoesNotExistException extends CallerErrorException
{
	public DoesNotExistException( Response res, Request req ) { super( res, req ); }
}
