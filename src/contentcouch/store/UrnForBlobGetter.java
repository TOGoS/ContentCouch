// -*- tab-width:4 -*-
package contentcouch.store;

import contentcouch.data.Blob;

public interface UrnForBlobGetter {
	public String getUrnForBlob( Blob blob );
}
