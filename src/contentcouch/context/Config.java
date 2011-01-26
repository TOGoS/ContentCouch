package contentcouch.context;

import contentcouch.contentaddressing.ContentAddressingScheme;
import contentcouch.contentaddressing.Schemes;
import contentcouch.misc.MapUtil;
import contentcouch.rdf.CCouchNamespace;

public class Config
{
	public static int getRdfDirectoryStyle() {
		return Integer.parseInt( (String)MapUtil.getKeyed( Context.getInstance(),
			CCouchNamespace.CFG_RDF_DIRECTORY_STYLE,
			CCouchNamespace.RDF_DIRECTORY_STYLE_NEW ) );
	}
	
	public static String getRdfSubjectPrefix() {
		return (String)MapUtil.getKeyed( Context.getInstance(),
			CCouchNamespace.CFG_RDF_SUBJECT_URI_PREFIX,
			CCouchNamespace.RDF_SUBJECT_URI_PREFIX );
	}

	public static ContentAddressingScheme getIdentificationScheme() {
		String schemeName = (String)MapUtil.getKeyed( Context.getInstance(), CCouchNamespace.CFG_ID_SCHEME, CCouchNamespace.ID_SCHEME_DEFAULT );
		ContentAddressingScheme cas = Schemes.getSchemeByName(schemeName);
		if( cas == null ) {
			throw new RuntimeException("No such content-addressing-scheme name '"+schemeName+"'");
		}
		return cas;
	}

}
