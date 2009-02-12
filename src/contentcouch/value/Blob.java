package contentcouch.value;

public interface Blob {
	public long getLength();
	public byte[] getData(long offset, int length);
}
