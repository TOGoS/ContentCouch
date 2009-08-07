package contentcouch.app.servlet;

import togos.rra.Arguments;
import togos.rra.BaseResponse;
import togos.rra.Request;
import togos.rra.Response;
import contentcouch.activefunctions.Explorify;
import contentcouch.builtindata.BuiltInData;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.UriUtil;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.DcNamespace;
import contentcouch.value.Blob;
import contentcouch.value.Directory;

public abstract class ProcessorComponent extends BaseSwfComponent {
	protected String externalPath;
	
	public ProcessorComponent(String externalPath) {
		this.externalPath = externalPath;
	}
	
	public static final String getProcessedUri(Arguments args) {
		return (String)args.getNamedArguments().get("uri");
	}
	
	protected String getInternalProcessingUri( String uri, String processor, String verb ) {
		return uri;
	}
	
	protected String getExternalPath() {
		return externalPath;
	}
	protected abstract String getProcessorActiveFunctionName();
	protected abstract String getVerb();
	
	public String getUriFor(Arguments args) {
		String uri = (String)args.getNamedArguments().get("uri");
		return getExternalPath() + "?uri=" + UriUtil.uriEncode(uri);
	}

	public Response handleRequest(Request request) {
		if( !request.getUri().startsWith(getExternalPath()) ) return BaseResponse.RESPONSE_UNHANDLED;
		String uri;
		if( request.getUri().startsWith(getExternalPath()+"/") ) {
			uri = "x-ccouch-repo:/" + request.getUri().substring(getExternalPath().length());
		} else {
			uri = (String)((Arguments)request.getContent()).getNamedArguments().get("uri");
		}
		if( uri == null ) {
			throw new RuntimeException("No URI provided in path or 'uri' parameter");
		}
		String internalUri = getInternalProcessingUri( uri, getProcessorActiveFunctionName(), getVerb() );
		Response subRes = forwardRequest(request,internalUri);
		if( subRes.getContent() instanceof Directory ) {
			subRes = Explorify.explorifyDirectory( uri, (Directory)subRes.getContent(),
				"<html><head><style>/*<!CDATA[*/\n" + BuiltInData.getString("default-page-style") + "/*]]>*/</style><body>\n", null );
		}
		
		BaseResponse res = new BaseResponse(subRes);
		
		String type = ValueUtil.getString(subRes.getContentMetadata().get(DcNamespace.DC_FORMAT));
		if( type == null && subRes.getContent() instanceof Blob ) {
			type = MetadataUtil.guessContentType((Blob)subRes.getContent());
			res.putContentMetadata(DcNamespace.DC_FORMAT, type);
		}
		return res;
	}

}
