package contentcouch.activefunctions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;

import togos.rra.BaseResponse;
import togos.rra.Response;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.Expression;
import contentcouch.blob.BlobUtil;
import contentcouch.blob.ByteArrayBlob;
import contentcouch.explorify.BaseUriProcessor;
import contentcouch.explorify.DirectoryPageGenerator;
import contentcouch.explorify.PageGenerator;
import contentcouch.explorify.RdfSourcePageGenerator;
import contentcouch.misc.MetadataUtil;
import contentcouch.value.Blob;
import contentcouch.value.Directory;

public class Explorify extends BaseActiveFunction {
	
	protected BaseResponse getPageGeneratorResult( PageGenerator pg ) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos));
		try {
			pg.write(pw);
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
		pw.close();
		Blob blob = new ByteArrayBlob(baos.toByteArray());
		return new BaseResponse(Response.STATUS_NORMAL, blob, pg.getContentType() );
	}
	
	protected Response explorifyDirectory(String uri, Directory d, Map m) {
		return getPageGeneratorResult(new DirectoryPageGenerator(uri, d, m, BaseUriProcessor.getInstance()));
	}
	
	protected Response explorifyXmlBlob(Blob b) {
		return getPageGeneratorResult(new RdfSourcePageGenerator(b, BaseUriProcessor.getInstance()));
	}
		
	public Response call(Map argumentExpressions) {
		Expression e = (Expression)argumentExpressions.get("operand");
		String uri = e.toUri();
		Response subRes = getArgumentResponse(argumentExpressions, "operand");
		String type="";
		if( subRes.getContent() instanceof Directory ) {
			return explorifyDirectory(uri, (Directory)subRes.getContent(), Collections.EMPTY_MAP);
		} else {
			Blob blob = BlobUtil.getBlob(subRes.getContent());
			if( ((type = MetadataUtil.getContentType(subRes)) != null &&	type.matches("application/(.*\\+)?xml")) ||
			    MetadataUtil.looksLikeRdfBlob(blob) )
			{
				return explorifyXmlBlob(blob);
			}
		}
		return subRes;
	}

	//// Path simplification ////
	
	protected String getPathArgumentName() {
		return "operand";
	}
}
