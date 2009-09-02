package contentcouch.app.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import togos.swf2.SwfHttpServlet;
import contentcouch.path.PathUtil;

public class ContentCouchExplorerServlet extends SwfHttpServlet {
	public ContentCouchExplorerServlet() {
		super(null); // requestHandler will be set up during init
	}
	
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
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ContentCouchExplorerRequestHandler ccerh = new ContentCouchExplorerRequestHandler();
		this.requestHandler = ccerh;
		File configFile = getConfigFile();
		String configFileUri = PathUtil.maybeNormalizeFileUri(configFile.getAbsolutePath());
		ccerh.init(configFileUri, PathUtil.maybeNormalizeFileUri(getServletContext().getRealPath("es1-resources")));
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
}
