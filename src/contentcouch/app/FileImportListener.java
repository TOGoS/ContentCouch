package contentcouch.app;

import java.io.File;

public interface FileImportListener {
	public void fileImported( File file, String urn );
}
