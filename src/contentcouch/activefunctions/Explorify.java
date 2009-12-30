package contentcouch.activefunctions;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import togos.mf.value.Blob;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.Expression;
import contentcouch.blob.BlobUtil;
import contentcouch.blob.InputStreamBlob;
import contentcouch.explorify.DirectoryPageGenerator;
import contentcouch.explorify.PageGenerator;
import contentcouch.explorify.RdfSourcePageGenerator;
import contentcouch.explorify.SlfSourcePageGenerator;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.value.Directory;
import contentcouch.value.Ref;
import contentcouch.xml.XML;

public class Explorify extends BaseActiveFunction {
	
	protected static BaseResponse getPageGeneratorResult( final PageGenerator pg ) {
		// TODO: Maybe use some sort of resettable-input-stream blob?
		try {
			PipedInputStream pis = new PipedInputStream();
			final PipedOutputStream pos = new PipedOutputStream(pis);
			final PrintWriter pw = new PrintWriter(new OutputStreamWriter(pos));
			new Thread(new Runnable() {
				public void run() {
					try {
						pg.write(pw);
					} catch( RuntimeException e ) {
						pw.println("<pre>");
						e.printStackTrace(pw);
						pw.println("</pre>");
						throw new RuntimeException(e);
					} finally {
						try {
							pw.close();
							pos.close();
						} catch( IOException e ) {
							throw new RuntimeException(e);
						}
					}
				}
			}).start();
			Blob blob = new InputStreamBlob(pis, -1);
			return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, blob, pg.getContentType() );
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	protected String getHeader(Request req, Map argumentExpressions) {
		return ValueUtil.getString(getArgumentValue(req, argumentExpressions, "header", null));
	}
	
	protected String getFooter(Request req, Map argumentExpressions) {
		return ValueUtil.getString(getArgumentValue(req, argumentExpressions, "footer", null));
	}
	
	public Response explorifyDirectory(Request req, Directory d ) {
		return getPageGeneratorResult(new DirectoryPageGenerator(d, req));
	}
	
	public Response explorifyDirectoryEntry(Request req, Map argumentExpressions, String uri, Directory.Entry de ) {
		return explorifyNonDirectory( req, argumentExpressions, uri,
			new BaseResponse( ResponseCodes.RESPONSE_NORMAL, BlobUtil.getBlob(de)) );
	}

	public Response explorifyXmlBlob(Request req, String uri, Blob b, String header, String footer ) {
		return getPageGeneratorResult(new RdfSourcePageGenerator(b, req));
	}
	
	public Response explorifySlfBlob(Request req, String uri, Blob b, String header, String footer ) {
		return getPageGeneratorResult(new SlfSourcePageGenerator(b, req));
	}
	
	public Response explorifyNonDirectory( Request req, Map argumentExpressions, String uri, Response subRes ) {
		Blob blob;
		String type;
		if( subRes.getContent() instanceof Ref ) {
			final String targetUri = ((Ref)subRes.getContent()).getTargetUri();
			return getPageGeneratorResult(new PageGenerator(req) {
				public void writeContent(PrintWriter w) {
					w.println("<div class=\"main-content\">");
					w.println("<h3>You found a Ref!</h3>");
					w.println("<p><a href=\""+XML.xmlEscapeAttributeValue(processUri("default",targetUri))+"\">"+XML.xmlEscapeText(targetUri)+"</a></p>");
					w.println("</div>");
				}
			});
		} else {
			blob = BlobUtil.getBlob(subRes.getContent());
			type = MetadataUtil.getContentType(subRes);
		}
		
		if( (type != null && type.matches("application/(.*\\+)?xml")) ||
		    (blob != null && MetadataUtil.looksLikeRdfBlob(blob)) )
		{
			return explorifyXmlBlob( req, uri, blob, getHeader(req, argumentExpressions), getFooter(req, argumentExpressions) );
		} else if( MetadataUtil.CT_SLF.equals(type) ) {
			return explorifySlfBlob( req, uri, blob, getHeader(req, argumentExpressions), getFooter(req, argumentExpressions) );
		} else if( type != null ) {
			return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, blob, type);
		} else {
			return subRes;
		}
	}
	
	public Response call(Request req, Map argumentExpressions) {
		Expression e = (Expression)argumentExpressions.get("operand");
		String uri = e.toUri();
		Response subRes = getArgumentResponse(req, argumentExpressions, "operand");
		if( subRes.getStatus() != ResponseCodes.RESPONSE_NORMAL ) return subRes;
		BaseRequest subReq = new BaseRequest(req);
		if( subRes.getContent() instanceof Directory ) {
			return explorifyDirectory(subReq, (Directory)subRes.getContent());
		} else if( subRes.getContent() instanceof Directory.Entry ) {
			return explorifyDirectoryEntry(subReq, argumentExpressions, uri, (Directory.Entry)subRes.getContent());
		} else {
			return explorifyNonDirectory(subReq, argumentExpressions, uri, subRes);
		}
	}

	//// Path simplification ////
	
	protected String getPathArgumentName() {
		return "operand";
	}
}
