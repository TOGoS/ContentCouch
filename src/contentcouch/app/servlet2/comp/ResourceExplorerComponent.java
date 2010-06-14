package contentcouch.app.servlet2.comp;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
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
import contentcouch.explorify.CCouchExplorerPageGenerator;
import contentcouch.explorify.DirectoryPageGenerator;
import contentcouch.explorify.PageGenerator;
import contentcouch.explorify.RdfSourcePageGenerator;
import contentcouch.explorify.SlfSourcePageGenerator;
import contentcouch.misc.MetadataUtil;
import contentcouch.misc.UriUtil;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.rdf.DcNamespace;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;
import contentcouch.value.Ref;
import contentcouch.xml.XML;

public class ResourceExplorerComponent extends BaseComponent {
	protected Map defaultifyConfig( Map config ) {
		return config;
	}
	
	public ResourceExplorerComponent(Map config) {
		properties = defaultifyConfig(config);
		
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
	
	protected Response explorifyDirectory(Request req, Response subRes, Directory d ) {
		return getPageGeneratorResult(new DirectoryPageGenerator(req, subRes, d));
	}
	
	protected Response explorifyDirectoryEntry(Request req, Directory.Entry de ) {
		return explorifyNonDirectory( req,
			new BaseResponse( ResponseCodes.RESPONSE_NORMAL, BlobUtil.getBlob(de)) );
	}

	protected Response explorifyXmlBlob(Request req, Response subRes, Blob b) {
		return getPageGeneratorResult(new RdfSourcePageGenerator(req, subRes, b));
	}
	
	protected Response explorifySlfBlob(Request req, Response subRes, Blob b) {
		return getPageGeneratorResult(new SlfSourcePageGenerator(req, subRes, b));
	}
	
	protected Response explorifyBlob( Request req, Response subRes, Blob blob ) {
		String type = MetadataUtil.getContentType(subRes);
		if( (type != null && type.matches("application/(.*\\+)?xml")) ||
		    (blob != null && MetadataUtil.looksLikeRdfBlob(blob)) )
		{
			return explorifyXmlBlob( req, subRes, blob );
		} else if( MetadataUtil.CT_SLF.equals(type) ) {
			return explorifySlfBlob( req, subRes, blob );
		} else if( type != null ) {
			BaseResponse res = new BaseResponse(subRes);
			res.putContentMetadata(DcNamespace.DC_FORMAT, type);
			return res;
		} else {
			return subRes;
		}
	}
	
	protected Response explorifyRef( Request req, Response subRes, Ref ref ) {
		final String targetUri = ((Ref)subRes.getContent()).getTargetUri();
		return getPageGeneratorResult(new CCouchExplorerPageGenerator(req, subRes) {
			public void writeContent(PrintWriter w) {
				w.println("<div class=\"main-content\">");
				w.println("<h3>You found a Ref!</h3>");
				w.println("<p><a href=\""+XML.xmlEscapeAttributeValue(getExternalUri(targetUri))+"\">"+XML.xmlEscapeText(targetUri)+"</a></p>");
				w.println("</div>");
			}
		});
	}
	
	protected Response explorifyNonDirectory( Request req, Response subRes ) {
		if( subRes.getContent() instanceof Ref ) {
			return explorifyRef( req, subRes, (Ref)subRes.getContent() );
		} else {
			Blob blob = BlobUtil.getBlob(subRes.getContent());
			return explorifyBlob( req, subRes, blob );
		}
	}

	protected Response explorify( Request req, Response subRes ) {
		if( subRes.getContent() instanceof Directory ) {
			return explorifyDirectory(req, subRes, (Directory)subRes.getContent());
		} else if( subRes.getContent() instanceof Directory.Entry ) {
			return explorifyDirectoryEntry(req, (Directory.Entry)subRes.getContent());
		} else {
			return explorifyNonDirectory(req, subRes);
		}
	}

	protected static HashSet NAMEANDURI = new HashSet();
	static {
		NAMEANDURI.add("name");
		NAMEANDURI.add("uri");
		NAMEANDURI.add("objectType");
	}
	
	public String getUriFor( Arguments args ) {
		String uri  = (String)args.getNamedArguments().get("uri");
		String name = (String)args.getNamedArguments().get("name");
		if( uri != null && name != null ) {
			// Then we can do it all cool!

			HashMap leftoverArgs = new HashMap(args.getNamedArguments());
			leftoverArgs.remove("uri");
			leftoverArgs.remove("name");
			String objectType = (String)leftoverArgs.remove("objectType");

			String path;
			// Path components need to be escaped *twice*
			// Once for the web server to decode (apparently)
			// Again for us to decode
			String uriEncoded = UriUtil.uriEncode(UriUtil.uriEncode(uri,UriUtil.PATH_SEGMENT_SAFE),UriUtil.PATH_SEGMENT_SAFE);
			if( CcouchNamespace.DIRECTORY.equals(objectType) || CcouchNamespace.TT_SHORTHAND_DIRECTORY.equals(objectType) ) {
				path = this.handlePath + "/" + uriEncoded + "/" + name + "/";
			} else {
				path = this.handlePath + "/" + uriEncoded + "/" + name;
			}
			return encodePathAndArguments(path, leftoverArgs);
		}
		return super.getUriFor(args);
	}
	
	public Response _call(Request req) {
		Arguments args = (Arguments)req.getContent();
		if( args == null ) args = new BaseArguments();
		
		String uri = (String)args.getNamedArguments().get("uri");
		String name = (String)args.getNamedArguments().get("name");
		String path = name;
		boolean allowRelativePaths = false;
		boolean preferRelativePaths = false;
		/** True to make more direct links to blobs even when other
		 * links are relative. */
		boolean alwaysRebaseBlobPaths = false;
		if( uri == null || uri == "" ) {
			String pi = req.getResourceName().substring(handlePath.length());
			if( pi.length() > 0 && pi.charAt(0) == '/' ) {
				preferRelativePaths = true;
				String[] parts = pi.substring(1).split("/",3);
				if( parts.length >= 1 && (uri = UriUtil.uriDecode(parts[0])).indexOf(':') != -1 ) {
					// .../<uri>/<name>[</more/stuff>]
					String baseUri = UriUtil.uriDecode(parts[0]);
					String baseName = (name == null) ? baseUri : name;
					if( parts.length >= 3 && pi.charAt(pi.length()-1) == '/' ) {
						allowRelativePaths = true;
					} else {
						allowRelativePaths = false;						
					}
					if( parts.length > 2 ) {
						uri = PathUtil.appendPath(uri, parts[2]);
						path = baseName + "/" + parts[2];
					}
				} else {
					// /<repo-name>/stuff/stuff/blah
					uri = "x-ccouch-repo:/" + pi;
					allowRelativePaths = pi.charAt(pi.length()-1) == '/';
				}
			} else {
				uri = "x-ccouch-repo://";
			}
		}
		if( name == null && path != null ) {
			name = path;
		}

		BaseRequest subReq = new BaseRequest(RequestVerbs.VERB_GET, uri);
		subReq.metadata = req.getMetadata();
		
		Response subRes = TheGetter.call(subReq);
		if( subRes.getStatus() != ResponseCodes.RESPONSE_NORMAL ) return subRes;

		BaseArguments subArgs = new BaseArguments(args);
		subArgs.putNamedArgument("uri", uri);
		subArgs.putNamedArgument("name", name);
		subArgs.putNamedArgument("path", path);
		subArgs.putNamedArgument( CCouchExplorerPageGenerator.ALLOW_RELATIVE_RESOURCE_URIS, Boolean.valueOf(allowRelativePaths));
		subArgs.putNamedArgument( CCouchExplorerPageGenerator.PREFER_RELATIVE_RESOURCE_URIS, Boolean.valueOf(preferRelativePaths));
		subArgs.putNamedArgument( CCouchExplorerPageGenerator.ALWAYS_REBASE_BLOB_URIS, Boolean.valueOf(alwaysRebaseBlobPaths));
		// SubReq is a clone of the user request with extra args filled in
		subReq = new BaseRequest(req);
		subReq.content = subArgs;
		return explorify( subReq, subRes );
	}
}
