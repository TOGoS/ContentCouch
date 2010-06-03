package contentcouch.contentaddressing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Schemes
{
	public static List allSchemes = new ArrayList();
	static {
		allSchemes.add(BitprintScheme.getInstance());
		allSchemes.add(TigerTreeScheme.getInstance());
		allSchemes.add(Sha1Scheme.getInstance());
	}
	
	public static ContentAddressingScheme getSchemeByName( String name ) {
		for( Iterator i=allSchemes.iterator(); i.hasNext(); ) {
			ContentAddressingScheme scheme = (ContentAddressingScheme)i.next();
			if( scheme.getSchemeShortName().equals(name) ) return scheme;
		}
		return null;
	}
}
