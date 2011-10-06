package contentcouch.framework.err;

import togos.mf.api.Request;
import togos.mf.api.Response;

public class NotFoundException extends AbnormalResponseException
{
	private static final long serialVersionUID = 1L;

	public NotFoundException( Response res, Request req ) { super( res, req ); }
}
