package contentcouch.app.servlet2;

import java.util.HashMap;

import contentcouch.app.servlet2.comp.JunkComponent;
import contentcouch.file.FileRequestHandler;
import contentcouch.path.PathUtil;
import togos.swf2.NameTranslator;
import togos.swf2.SwfFrontRequestHandler;
import togos.swf2.SwfHttpServlet;

public class CCouchServlet extends SwfHttpServlet {
	public CCouchServlet() {}
	
	public void init() {
		SwfFrontRequestHandler frh = new SwfFrontRequestHandler();
		this.requestHandler = frh;
		
		HashMap junkConfig = new HashMap();
		junkConfig.put("path", SwfHttpServlet.SERVLET_PATH_URI_PREFIX+"/junk/");
		frh.putComponent("junk", new JunkComponent(junkConfig));
		
		frh.putComponent("resources", new NameTranslator(
			SwfHttpServlet.SERVLET_PATH_URI_PREFIX+"/",
			PathUtil.maybeNormalizeFileUri(getServletContext().getRealPath("es2-resources")),
			new FileRequestHandler(),
			"index.html"
		));
	}
}
