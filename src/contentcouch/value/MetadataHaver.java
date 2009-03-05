package contentcouch.value;

import java.util.Map;

public interface MetadataHaver {
	public Map getMetadata();
	public Object getMetadata(String name);
	public void putMetadata(String name, Object value);
}
