package contentcouch.app.servlet2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import togos.mf.api.Request;
import togos.mf.base.BaseRequest;
import togos.swf2.NameTranslator;
import togos.swf2.SwfFrontRequestHandler;
import togos.swf2.SwfHttpServlet;
import togos.swf2.SwfNamespace;
import contentcouch.app.servlet2.comp.JunkComponent;
import contentcouch.app.servlet2.comp.PhotoAlbumComponent;
import contentcouch.app.servlet2.comp.RawResourceComponent;
import contentcouch.app.servlet2.comp.ResourceExplorerComponent;
import contentcouch.file.FileRequestHandler;
import contentcouch.framework.TheGetter;
import contentcouch.misc.FormatGuesser;
import contentcouch.path.PathUtil;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.repository.MetaRepoConfig;

public class CCouchServlet extends SwfHttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public CCouchServlet() {}

	protected void copyFile( File src, File dest ) throws IOException {
		FileInputStream is = new FileInputStream(src);
		FileOutputStream os = new FileOutputStream(dest);
		try {
			byte[] buf = new byte[512];
			int len;
			while( (len = is.read(buf)) > 0 ) {
				os.write(buf, 0, len);
			}
		} finally {
			is.close();
			os.close();
		}
	}

	protected File getConfigFile() {
		String webPath = getServletContext().getRealPath("");
		File configFile = new File(webPath + "/repo-config");
		File configTemplateFile = new File(webPath + "/repo-config.template");
		if( !configFile.exists() ) {
			try {
				copyFile(configTemplateFile, configFile);
			} catch( IOException e ) {
				throw new RuntimeException("Failed to copy " + configTemplateFile.getPath() + " to " + configFile.getPath(), e);
			}
		}
		return configFile;
	}
	
	protected MetaRepoConfig metaRepoConfig = new MetaRepoConfig();
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		SwfFrontRequestHandler frh = new SwfFrontRequestHandler();
		this.requestHandler = frh;
		
		HashMap junkConfig = new HashMap();
		junkConfig.put("path", SwfNamespace.SERVLET_PATH_URI_PREFIX+"/junk/");
		frh.putComponent("junk", new JunkComponent(junkConfig));
		
		HashMap albumConfig = new HashMap();
		albumConfig.put("path", SwfNamespace.SERVLET_PATH_URI_PREFIX+"/album");
		frh.putComponent("album", new PhotoAlbumComponent(albumConfig));

		HashMap rawConfig = new HashMap();
		rawConfig.put("path", SwfNamespace.SERVLET_PATH_URI_PREFIX+"/raw");
		frh.putComponent("raw", new RawResourceComponent(rawConfig));

		HashMap exploreConfig = new HashMap();
		exploreConfig.put("path", SwfNamespace.SERVLET_PATH_URI_PREFIX+"/explore");
		frh.putComponent("explore", new ResourceExplorerComponent(exploreConfig));
		
		HashMap ntConfig = new HashMap();
		ntConfig.put("autoAppendPaths", "");
		ntConfig.put("directoryIndex", "index.html");
		ntConfig.put("path", SwfNamespace.SERVLET_PATH_URI_PREFIX+"/");
		ntConfig.put("translatedPath", PathUtil.maybeNormalizeFileUri(getServletContext().getRealPath("resources"))+"/");
		frh.putComponent("resources", new NameTranslator(new FormatGuesser(new FileRequestHandler()),ntConfig));

		File configFile = getConfigFile();
		String configFileUri = PathUtil.maybeNormalizeFileUri(configFile.getAbsolutePath());
		TheGetter.globalInstance = metaRepoConfig.getRequestKernel();
		TheGetter.globalInstance = metaRepoConfig.getRequestKernel();
		metaRepoConfig.handleArguments(new String[]{"-file",configFileUri}, 0, configFileUri);
	}
	
	protected void doGeneric( Request req, HttpServletResponse response ) throws ServletException, IOException {
		BaseRequest subReq = new BaseRequest(req);
		subReq.metadata = metaRepoConfig.config;
		subReq.putMetadata(CCouchNamespace.REQ_CACHE_SECTOR, "webcache");
		/*
		for( Iterator i=metaRepoConfig.config.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			response.getWriter().write( e.getKey().toString() + " = " + e.getValue().toString() + "\n" );
		}
		*/
		super.doGeneric( subReq, response );
		//response.getWriter().println( JSON.encodeObject(subReq.getContextVars().get(SwfNamespace.CTX_CONFIG)));
	}
}
