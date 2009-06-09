package contentcouch.activefunctions;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.Expression;
import contentcouch.blob.BlobUtil;
import contentcouch.blob.InputStreamBlob;
import contentcouch.explorify.BaseUriProcessor;
import contentcouch.explorify.DirectoryPageGenerator;
import contentcouch.explorify.PageGenerator;
import contentcouch.explorify.RdfSourcePageGenerator;
import contentcouch.explorify.SlfSourcePageGenerator;
import contentcouch.misc.MetadataUtil;
import contentcouch.value.Blob;
import contentcouch.value.Directory;

public class Explorify extends BaseActiveFunction {
	
	protected BaseResponse getPageGeneratorResult( final PageGenerator pg ) {
		// TODO: Maybe use some sort of resettable-input-stream blob?
		try {
			PipedInputStream pis = new PipedInputStream();
			final PipedOutputStream pos = new PipedOutputStream(pis);
			final PrintWriter pw = new PrintWriter(new OutputStreamWriter(pos));
			new Thread(new Runnable() {
				public void run() {
					try {
						pg.write(pw);
						pw.close();
						pos.close();
					} catch( IOException e ) {
						throw new RuntimeException(e);
					}
				}
			}).start();
			Blob blob = new InputStreamBlob(pis, -1);
			return new BaseResponse(Response.STATUS_NORMAL, blob, pg.getContentType() );
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	protected Response explorifyDirectory(String uri, Directory d, Map m) {
		return getPageGeneratorResult(new DirectoryPageGenerator(uri, d, m, BaseUriProcessor.getInstance()));
	}
	
	protected Response explorifyXmlBlob(Blob b) {
		return getPageGeneratorResult(new RdfSourcePageGenerator(b, BaseUriProcessor.getInstance()));
	}
	
	protected Response explorifySlfBlob(Blob b) {
		return getPageGeneratorResult(new SlfSourcePageGenerator(b, ""));
	}
		
	public Response call(Map argumentExpressions) {
		Expression e = (Expression)argumentExpressions.get("operand");
		String uri = e.toUri();
		Response subRes = getArgumentResponse(argumentExpressions, "operand");
		if( subRes.getContent() instanceof Directory ) {
			return explorifyDirectory(uri, (Directory)subRes.getContent(), Collections.EMPTY_MAP);
		} else {
			Blob blob = BlobUtil.getBlob(subRes.getContent());
			String type = MetadataUtil.getContentType(subRes);
			if( (type != null && type.matches("application/(.*\\+)?xml")) ||
			    MetadataUtil.looksLikeRdfBlob(blob) )
			{
				return explorifyXmlBlob( blob );
			} else if( MetadataUtil.CT_SLF.equals(type) ) {
				return explorifySlfBlob( blob );
			}
		}
		return subRes;
	}

	//// Path simplification ////
	
	protected String getPathArgumentName() {
		return "operand";
	}
}
