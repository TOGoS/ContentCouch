package togos.swf2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import contentcouch.blob.BlobUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.DcNamespace;

import togos.mf.RequestVerbs;
import togos.mf.ResponseCodes;
import togos.mf.Request;
import togos.mf.RequestHandler;
import togos.mf.Response;
import togos.mf.base.BaseArguments;
import togos.mf.base.BaseRequest;

public class SwfHttpServlet extends HttpServlet {
	protected RequestHandler requestHandler;
	public static final String SERVLET_PATH_URI_PREFIX = "x-servlet-path:";
	
	public SwfHttpServlet(RequestHandler rh) {
		this.requestHandler = rh;
	}

	protected void doGeneric( Request subReq, HttpServletResponse response ) throws ServletException, IOException {
		try {
			Response subRes = requestHandler.call(subReq);
			String type = ValueUtil.getString(subRes.getContentMetadata().get(DcNamespace.DC_FORMAT));
			switch( subRes.getStatus() ) {
			case( ResponseCodes.RESPONSE_NORMAL ): break;
			case( ResponseCodes.RESPONSE_DOESNOTEXIST ): case( ResponseCodes.RESPONSE_UNHANDLED ):
				response.sendError(404, "Resource Not Found");
				response.addHeader("Content-Type", "text/plain");
				response.getWriter().println("Could not find resource: " + subReq.getUri() );
				break;
			case( ResponseCodes.RESPONSE_CALLER_ERROR ):
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
		// TODO: Update to return blobs when content is not recognized as argument map
		HashMap namedArguments = new HashMap();
		for( Iterator i=req.getParameterMap().entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			namedArguments.put( e.getKey(), ((String[])e.getValue())[0] );
		}
		return new BaseArguments( null, namedArguments );
	}
	
	protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
		BaseRequest subReq = new BaseRequest();
		subReq.verb = RequestVerbs.VERB_GET;
		subReq.uri = SERVLET_PATH_URI_PREFIX + req.getServletPath();
		subReq.content = parseContent(req);
		subReq.putMetadata(SwfNamespace.HTTP_SERVLET_REQUEST, req);
		subReq.putMetadata(SwfNamespace.HTTP_SERVLET_RESPONSE, resp);
		doGeneric(subReq, resp);
	}

	protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
		BaseRequest subReq = new BaseRequest();
		subReq.verb = RequestVerbs.VERB_POST;
		subReq.uri = SERVLET_PATH_URI_PREFIX + req.getServletPath();
		subReq.content = parseContent(req);
		subReq.putMetadata(SwfNamespace.HTTP_SERVLET_REQUEST, req);
		subReq.putMetadata(SwfNamespace.HTTP_SERVLET_RESPONSE, resp);
		doGeneric(subReq, resp);
	}

	protected void doPut( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
		BaseRequest subReq = new BaseRequest();
		subReq.verb = RequestVerbs.VERB_PUT;
		subReq.uri = SERVLET_PATH_URI_PREFIX + req.getServletPath();
		subReq.content = parseContent(req);
		subReq.putMetadata(SwfNamespace.HTTP_SERVLET_REQUEST, req);
		subReq.putMetadata(SwfNamespace.HTTP_SERVLET_RESPONSE, resp);
		doGeneric(subReq, resp);
	}
}
