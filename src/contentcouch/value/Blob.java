package contentcouch.value;

/** A Blob is an abstract sequence of bytes.
 *  Its contents should be considered immutable. */
public interface Blob {
	public long getLength();
	public byte[] getData(long offset, int length);
}
