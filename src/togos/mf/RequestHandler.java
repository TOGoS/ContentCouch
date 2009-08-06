package togos.mf;

import togos.rra.Request;
import togos.rra.Response;

/** Used to send messages and/or request things from a remote server */
public interface RequestHandler {
	/** Send a request, but do not wait for any response. */
	public void send( Request req );
	/** Send a request and expect a response in return.
	 * If no response is returned by the underlying protocol, this should
	 * return null.  Otherwise the semantics are identical to {@link togos.rra.RequestHandler#handleRequest(Request)} */
	public Response call( Request req );
	/** Send a request and return a MessageIterator that will give every event
	 * and message sent back associated with the request */
	public MessageIterator open( Request req );
}
