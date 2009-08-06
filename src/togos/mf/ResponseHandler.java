package togos.mf;

import togos.rra.Response;

/** Used to send messages back to the requester */
public interface ResponseHandler {
	public void sendEvent( Event e );
	public void sendResponse( Response res );
	public void close();
}
