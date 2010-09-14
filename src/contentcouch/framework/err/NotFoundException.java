package contentcouch.framework.err;

import togos.mf.api.Request;
import togos.mf.api.Response;

public class NotFoundException extends AbnormalResponseException
{
	public NotFoundException( Response res, Request req ) { super( res, req ); }
}
