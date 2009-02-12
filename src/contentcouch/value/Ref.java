/**
 * 
 */
package contentcouch.value;

import java.util.Map;

public class Ref {
	public String targetUri;
	public Map targetMetadata;
	public Ref(String targetUri) {
		this.targetUri = targetUri;
	}
}