package contentcouch.data;

public interface Blob {
	public long getLength();
	public byte[] getData(long offset, int length);
}
