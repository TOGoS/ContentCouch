package togos.mf;


/** Used to send messages and/or request things between components */
public interface RequestHandler {
	/** Send a request, but do not wait for any response.
	 * Should return false if this RequestHandler does not know what to do with the request. */
	public boolean send( Request req );
	/** Send a request and expect a response in return.
	 * Should return null if this RequestHandler does not know what to do with the request. */
	public Response call( Request req );
	/** Send a request and return a MessageIterator that will give every event
	 * and message sent back associated with the request.
	 * Should return null if this RequestHandler does not know what to do with the request. */
	public ResponseSession open( Request req );
}