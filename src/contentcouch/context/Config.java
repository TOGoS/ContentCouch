package contentcouch.context;

import contentcouch.contentaddressing.ContentAddressingScheme;
import contentcouch.contentaddressing.Schemes;
import contentcouch.misc.MapUtil;
import contentcouch.rdf.CcouchNamespace;

public class Config
{
	public static int getRdfDirectoryStyle() {
		return Integer.parseInt( (String)MapUtil.getKeyed( Context.getInstance(),
			CcouchNamespace.CFG_RDF_DIRECTORY_STYLE,
			CcouchNamespace.RDF_DIRECTORY_STYLE_OLD ) );
	}
	
	public static String getRdfSubjectPrefix() {
		return (String)MapUtil.getKeyed( Context.getInstance(),
			CcouchNamespace.CFG_RDF_SUBJECT_URI_PREFIX,
			CcouchNamespace.RDF_SUBJECT_URI_PREFIX_OLD );
	}

	public static ContentAddressingScheme getIdentificationScheme() {
		String schemeName = (String)MapUtil.getKeyed( Context.getInstance(), CcouchNamespace.CFG_ID_SCHEME, CcouchNamespace.ID_SCHEME_DEFAULT );
		ContentAddressingScheme cas = Schemes.getSchemeByName(schemeName);
		if( cas == null ) {
			throw new RuntimeException("No such content-addressing-scheme name '"+schemeName+"'");
		}
		return cas;
	}

}
