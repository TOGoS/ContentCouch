package contentcouch.contentaddressing;

import java.util.ArrayList;
import java.util.List;

public class Schemes
{
	public static List allSchemes = new ArrayList();
	static {
		allSchemes.add(BitprintScheme.getInstance());
		allSchemes.add(TigerTreeScheme.getInstance());
		allSchemes.add(Sha1Scheme.getInstance());
	}
}
