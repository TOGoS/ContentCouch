package contentcouch.graphics.activefunctions;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import togos.mf.value.Blob;
import contentcouch.active.BaseActiveFunction;
import contentcouch.active.expression.Expression;
import contentcouch.blob.BlobUtil;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.store.TheGetter;
import contentcouch.value.BaseRef;
import contentcouch.value.Ref;

public class Thumbnail extends BaseActiveFunction {
	public String getConvertExe() {
		return "C:\\apps\\imagemagick-6.3.7-Q16\\convert.exe";
	}
	
	protected Blob getProcessOutput( Process proc ) throws IOException {
		return BlobUtil.readInputStreamIntoBlob(proc.getInputStream(), 8*1024*1024 );
	}
	
	protected Blob getProcessError( Process proc ) throws IOException {
		return BlobUtil.readInputStreamIntoBlob(proc.getErrorStream(), 2048);
	}
	
	protected String joinArgs( String[] args ) {
		String res = "";
		for( int i=0; i<args.length; ++i ) {
			String arg = args[i];
			if( arg.matches(".*?[ \\*\\<\\>].*") )  {
				arg = "\"" + arg + "\"";
			}
			if( res.length() > 0 ) res += " ";
			res += arg;
		}
		return res;
	}
	
	protected Blob execWithBlobIO( String[] args, Blob input )
		throws IOException
	{
		Process proc = null;
		try {
			proc = Runtime.getRuntime().exec(args);
			if( input != null ) {
				OutputStream  os = proc.getOutputStream();
				BlobUtil.writeBlobToOutputStream(input, os);
				os.close();
			}
			Blob output = getProcessOutput( proc );
			try {
				if( proc.waitFor() != 0 ) {
					String errText;
					try {
						errText = "\n"+ValueUtil.getString(getProcessError(proc));
					} catch( IOException e ) { errText = ""; }
					throw new IOException("Process exited with code "+proc.exitValue()+": " + joinArgs(args) + errText);
				}
			} catch( InterruptedException e ) {
				throw new RuntimeException( e );
			}
			return output;
		} catch( IOException e ) {
			String errText = "";
			if( proc != null ) try {
				errText = "\n"+ValueUtil.getString(getProcessError(proc));
			} catch( IOException e2 ) { }
			throw new RuntimeException("Error while executing "+joinArgs(args)+errText, e );
		}
	}
	
	public Response call(Request req, Map argumentExpressions) {
		Expression operandExpression = (Expression)argumentExpressions.get("operand");
		boolean cacheable = operandExpression.isConstant(); 
		String id = TheGetter.identify(operandExpression, Collections.EMPTY_MAP);
		String resultCacheUri = "x-ccouch-repo:function-result-cache/thumbnails/"+UriUtil.uriEncode(id);
		Object cached = cacheable ? TheGetter.get(resultCacheUri) : null;
		String thumbnailUri;
		if( cached == null ) {
			int twidth = 128;
			int theight = 128;
			Blob input = (Blob)getArgumentValue(req, argumentExpressions, "operand", null);
			if( input == null ) throw new RuntimeException("No value for operand");
			Blob output;
			try {
				String inputFilename;
				Blob inputBlob;
				if( input instanceof File ) {
					inputFilename = ((File)input).getPath();
					inputBlob = null;
				} else {
					inputFilename = "-";
					inputBlob = input;
				}
				output = execWithBlobIO( new String[]{getConvertExe(),inputFilename,"-thumbnail",(twidth+"x"+theight+">"),"jpg:-"}, inputBlob );
			} catch( IOException e ) {
				throw new RuntimeException("IOException while trying to generate thumbnail");
			}
			
			if( cacheable ) {
				BaseRequest storeReq = new BaseRequest(RequestVerbs.VERB_POST, "x-ccouch-repo:data", output, Collections.EMPTY_MAP);
				storeReq.putMetadata( CcouchNamespace.REQ_STORE_SECTOR, "function-results" );
				storeReq.putMetadata( CcouchNamespace.REQ_FILEMERGE_METHOD, CcouchNamespace.REQ_FILEMERGE_IGNORE );
				Response storeRes = TheGetter.call(storeReq);
				thumbnailUri = (String)storeRes.getMetadata().get(CcouchNamespace.RES_STORED_IDENTIFIER);
				
				TheGetter.put(resultCacheUri, new BaseRef(thumbnailUri));
			} else {
				return new BaseResponse( ResponseCodes.RESPONSE_NORMAL, output );
			}
		} else {
			if( cached instanceof Ref ) { // oughta be
				thumbnailUri = ((Ref)cached).getTargetUri();
			} else {
				throw new RuntimeException("Object returned from cache was not ref: " + cached);
			}
		}
		if( thumbnailUri == null ) {
			throw new RuntimeException( "No thumbnailUri" );
		}
		
		return TheGetter.call( new BaseRequest( RequestVerbs.VERB_GET, thumbnailUri ) );
		
		//return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, "Thumbnail of "+id, "text/plain");
	}
}
