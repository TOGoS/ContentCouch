package togos.swf2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import togos.mf.api.CallHandler;
import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseArguments;
import togos.mf.base.BaseRequest;
import togos.mf.value.Blob;
import contentcouch.blob.BlobUtil;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.rdf.DcNamespace;

public class SwfHttpServlet extends HttpServlet {
	protected CallHandler requestHandler;
	public SwfHttpServlet() { }
	
	public SwfHttpServlet(CallHandler rh) {
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
				response.getWriter().println("Could not find resource: " + subReq.getResourceName() );
				break;
			case( ResponseCodes.RESPONSE_CALLER_ERROR ):
				response.sendError(400, "User Error"); break;
			default:
				response.sendError(500, "RRA Error " + subRes.getStatus()); break;
			}
			Blob b = BlobUtil.getBlob( subRes.getContent() );
			if( b == null ) {
				response.setHeader("Content-Type", "text/plain");
				response.getWriter().println("No content");
			} else {
				// Copy over cache-related headers
				if( MetadataUtil.isEntryTrue(subRes.getMetadata(), CcouchNamespace.RES_CACHEABLE) ) {
					response.setHeader("Pragma", "cache");
					response.setHeader("Cache-Control", "cache");
				}
				// Copy over http-equivalent headers
				for( Iterator mdi=subRes.getMetadata().entrySet().iterator(); mdi.hasNext(); ) {
					Map.Entry e = (Map.Entry)mdi.next();
					String mdk = ValueUtil.getString(e.getKey()); 
					if( mdk.startsWith( SwfNamespace.RES_HTTP_EQUIV ) ) {
						response.setHeader( mdk.substring(SwfNamespace.RES_HTTP_EQUIV.length()),
							ValueUtil.getString(e.getValue()) ); 
					}
				}
				if( type != null ) response.setHeader("Content-Type", type);
				long len = b.getLength();
				if( len != -1 ) {
					response.setHeader("Content-Length", ""+len);
				}
				BlobUtil.writeBlobToOutputStream( b, response.getOutputStream() );
			}
		} catch( Exception e ) {
			response.setHeader("Content-Type", "text/plain");
			e.printStackTrace(response.getWriter());
			e.printStackTrace(System.err);
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
		subReq.uri = SwfNamespace.SERVLET_PATH_URI_PREFIX + req.getPathInfo();
		subReq.content = parseContent(req);
		subReq.putMetadata(SwfNamespace.HTTP_SERVLET_REQUEST, req);
		subReq.putMetadata(SwfNamespace.HTTP_SERVLET_RESPONSE, resp);
		doGeneric(subReq, resp);
	}

	protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
		BaseRequest subReq = new BaseRequest();
		subReq.verb = RequestVerbs.VERB_POST;
		subReq.uri = SwfNamespace.SERVLET_PATH_URI_PREFIX + req.getPathInfo();
		subReq.content = parseContent(req);
		subReq.putMetadata(SwfNamespace.HTTP_SERVLET_REQUEST, req);
		subReq.putMetadata(SwfNamespace.HTTP_SERVLET_RESPONSE, resp);
		doGeneric(subReq, resp);
	}

	protected void doPut( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
		BaseRequest subReq = new BaseRequest();
		subReq.verb = RequestVerbs.VERB_PUT;
		subReq.uri = SwfNamespace.SERVLET_PATH_URI_PREFIX + req.getPathInfo();
		subReq.content = parseContent(req);
		subReq.putMetadata(SwfNamespace.HTTP_SERVLET_REQUEST, req);
		subReq.putMetadata(SwfNamespace.HTTP_SERVLET_RESPONSE, resp);
		doGeneric(subReq, resp);
	}
}
