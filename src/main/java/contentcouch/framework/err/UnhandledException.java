package contentcouch.framework.err;

import togos.mf.api.Request;
import togos.mf.api.Response;

public class UnhandledException extends NotFoundException
{
	private static final long serialVersionUID = 1L;
	
	public UnhandledException( Response res, Request req ) { super( res, req ); }
}
