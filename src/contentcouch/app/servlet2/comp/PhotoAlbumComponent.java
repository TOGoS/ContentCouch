package contentcouch.app.servlet2.comp;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import togos.mf.api.Request;
import togos.mf.api.RequestVerbs;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseArguments;
import togos.mf.base.BaseRequest;
import togos.mf.base.BaseResponse;
import togos.mf.value.Arguments;
import togos.mf.value.Blob;
import contentcouch.blob.BlobUtil;
import contentcouch.blob.InputStreamBlob;
import contentcouch.explorify.PageGenerator;
import contentcouch.explorify.RdfSourcePageGenerator;
import contentcouch.explorify.SlfSourcePageGenerator;
import contentcouch.misc.MetadataUtil;
import contentcouch.photoalbum.activefunctions.AlbumPage;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;
import contentcouch.value.Ref;
import contentcouch.xml.XML;

public class PhotoAlbumComponent extends BaseComponent {
	public PhotoAlbumComponent(Map config) {
		properties = new HashMap(config);
		if( properties.get("title") == null ) properties.put("title", "Photo Album Viewer");
		if( properties.get("path") == null ) properties.put("path", "x-internal:album");
		
		this.handlePath = (String)properties.get("path");
	}
	
	public Map getProperties() {
		return properties;
	}
	
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
	
	public Response explorifyDirectory(Request req, Directory d ) {
		//return getPageGeneratorResult(new DirectoryPageGenerator(d, req));
		return getPageGeneratorResult(new AlbumPage.AlbumPageGenerator(d, req));
	}
	
	public Response explorifyDirectoryEntry(Request req, Directory.Entry de ) {
		return explorifyNonDirectory( req,
			new BaseResponse( ResponseCodes.RESPONSE_NORMAL, BlobUtil.getBlob(de)) );
	}

	public Response explorifyXmlBlob(Request req, Blob b) {
		return getPageGeneratorResult(new RdfSourcePageGenerator(b, req));
	}
	
	public Response explorifySlfBlob(Request req, Blob b) {
		return getPageGeneratorResult(new SlfSourcePageGenerator(b, req));
	}
	
	public Response explorifyNonDirectory( Request req, Response subRes ) {
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
			return explorifyXmlBlob( req, blob );
		} else if( MetadataUtil.CT_SLF.equals(type) ) {
			return explorifySlfBlob( req, blob );
		} else if( type != null ) {
			return new BaseResponse(ResponseCodes.RESPONSE_NORMAL, blob, type);
		} else {
			return subRes;
		}
	}

	protected Response explorify( Request subReq, Response subRes ) {
		if( subRes.getContent() instanceof Directory ) {
			return explorifyDirectory(subReq, (Directory)subRes.getContent());
		} else if( subRes.getContent() instanceof Directory.Entry ) {
			return explorifyDirectoryEntry(subReq, (Directory.Entry)subRes.getContent());
		} else {
			return explorifyNonDirectory(subReq, subRes);
		}
	}
	
	public Response _call(Request req) {
		Arguments args = (Arguments)req.getContent();
		if( args == null ) args = new BaseArguments();
		
		String uri = (String)args.getNamedArguments().get("uri");
		if( uri == null || uri == "" ) uri = "x-ccouch-repo://";

		BaseRequest subReq = new BaseRequest(RequestVerbs.VERB_GET, uri);
		subReq.contextVars = req.getContextVars();
		
		Response subRes = TheGetter.call(subReq);
		if( subRes.getStatus() != ResponseCodes.RESPONSE_NORMAL ) return subRes;

		BaseArguments subArgs = new BaseArguments(args);
		subArgs.putNamedArgument("uri", uri); // In case it was defaulted
		subReq = new BaseRequest(req);
		subReq.content = subArgs;
		return explorify( subReq, subRes );
	}
}
