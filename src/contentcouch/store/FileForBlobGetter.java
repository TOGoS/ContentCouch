// -*- tab-width:4 -*-
package contentcouch.store;

import java.io.File;

import contentcouch.data.Blob;

public interface FileForBlobGetter {
    public File getFileForBlob( Blob blob );
}
