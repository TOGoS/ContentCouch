// -*- tab-width:4 -*-
package contentcouch.store;

import contentcouch.value.Blob;

public interface UrnForBlobGetter {
	public String getUrnForBlob( Blob blob );
}
