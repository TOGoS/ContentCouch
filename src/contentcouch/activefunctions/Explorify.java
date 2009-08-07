package contentcouch.activefunctions;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.Map;

import togos.mf.ResponseCodes;
import togos.mf.Response;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.Context;
import contentcouch.active.expression.Expression;
import contentcouch.blob.BlobUtil;
import contentcouch.blob.InputStreamBlob;
import contentcouch.explorify.DirectoryPageGenerator;
import contentcouch.explorify.PageGenerator;
import contentcouch.explorify.RdfSourcePageGenerator;
import contentcouch.explorify.SlfSourcePageGenerator;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.value.Blob;
import contentcouch.value.Directory;

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
	
	protected static String getHeader(Map argumentExpressions) {
		return ValueUtil.getString(getArgumentValue(argumentExpressions, "header", null));
	}
	
	protected static String getFooter(Map argumentExpressions) {
		return ValueUtil.getString(getArgumentValue(argumentExpressions, "footer", null));
	}
	
	public static Response explorifyDirectory(String uri, Directory d, String header, String footer ) {
		return getPageGeneratorResult(new DirectoryPageGenerator(d, uri, Context.getSnapshot(), header, footer ));
	}
	
	public static Response explorifyXmlBlob(String uri, Blob b, String header, String footer ) {
		return getPageGeneratorResult(new RdfSourcePageGenerator(b, uri, Context.getSnapshot(), header, footer ));
	}
	
	public static Response explorifySlfBlob(String uri, Blob b, String header, String footer ) {
		return getPageGeneratorResult(new SlfSourcePageGenerator(b, uri, Context.getSnapshot(), header, footer ));
	}
		
	public Response call(Map argumentExpressions) {
		Expression e = (Expression)argumentExpressions.get("operand");
		String uri = e.toUri();
		Response subRes = getArgumentResponse(argumentExpressions, "operand");
		if( subRes.getStatus() != ResponseCodes.RESPONSE_NORMAL ) return subRes;
		Context.push("processed-uri", uri);
		try {
			if( subRes.getContent() instanceof Directory ) {
				return explorifyDirectory(uri, (Directory)subRes.getContent(), getHeader(argumentExpressions), getFooter(argumentExpressions));
			} else {
				Blob blob = BlobUtil.getBlob(subRes.getContent());
				String type = MetadataUtil.getContentType(subRes);
				if( (type != null && type.matches("application/(.*\\+)?xml")) ||
				    MetadataUtil.looksLikeRdfBlob(blob) )
				{
					return explorifyXmlBlob( uri, blob, getHeader(argumentExpressions), getFooter(argumentExpressions) );
				} else if( MetadataUtil.CT_SLF.equals(type) ) {
					return explorifySlfBlob( uri, blob, getHeader(argumentExpressions), getFooter(argumentExpressions) );
				} else if( type != null ) {
					return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, blob, type);
				}
			}
		} finally {
			Context.pop("processed-uri");
		}
		return subRes;
	}

	//// Path simplification ////
	
	protected String getPathArgumentName() {
		return "operand";
	}
}
