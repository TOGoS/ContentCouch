package togos.swf2;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import contentcouch.blob.BlobUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.DcNamespace;

import togos.rra.BaseArguments;
import togos.rra.BaseRequest;
import togos.rra.Request;
import togos.rra.RequestHandler;
import togos.rra.Response;

public class SwfHttpServlet extends HttpServlet {
	protected RequestHandler requestHandler;
	public static final String SERVLET_PATH_URI_PREFIX = "x-servlet-path:";
	
	public SwfHttpServlet(RequestHandler rh) {
		this.requestHandler = rh;
	}

	protected void doGeneric( Request subReq, HttpServletResponse response ) throws ServletException, IOException {
		try {
			Response subRes = requestHandler.handleRequest(subReq);
			String type = ValueUtil.getString(subRes.getContentMetadata().get(DcNamespace.DC_FORMAT));
			switch( subRes.getStatus() ) {
			case( Response.STATUS_NORMAL ): break;
			case( Response.STATUS_DOESNOTEXIST ): case( Response.STATUS_UNHANDLED ):
				response.sendError(404, "Resource Not Found");
				response.addHeader("Content-Type", "text/plain");
				response.getWriter().println("Could not find resource: " + subReq.getUri() );
				break;
			case( Response.STATUS_USERERROR ):
				response.sendError(400, "User Error"); break;
			default:
				response.sendError(500, "RRA Error " + subRes.getStatus()); break;
			}
			if( type != null ) response.setHeader("Content-Type", type);
			BlobUtil.writeBlobToOutputStream( BlobUtil.getBlob( subRes.getContent() ), response.getOutputStream() );
		} catch( Exception e ) {
			response.setHeader("Content-Type", "text/plain");
			e.printStackTrace(response.getWriter());
		}
	}
	
	protected Object parseContent( HttpServletRequest req ) {
		// TODO: Update to return blobs when content is not recognised as argument map
		return new BaseArguments( null, req.getParameterMap() );
	}
	
	protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
		BaseRequest subReq = new BaseRequest();
		subReq.verb = Request.VERB_GET;
		subReq.uri = SERVLET_PATH_URI_PREFIX + req.getServletPath();
		subReq.content = parseContent(req);
		subReq.putMetadata(SwfNamespace.HTTP_SERVLET_REQUEST, req);
		subReq.putMetadata(SwfNamespace.HTTP_SERVLET_RESPONSE, resp);
		doGeneric(subReq, resp);
	}
}
