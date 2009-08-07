package togos.mf.value;

/** A Blob is an abstract sequence of bytes.
 *  Its contents should be considered immutable. */
public interface Blob {
	/** Return the total number of bytes contained in the blob.
	 * If the value is unknown, this may return -1. */
	public long getLength();
	/** Returns an array of bytes from the blob starting at the given offset.
	 * The length of the returned array may be anything greater than 0.
	 * The length of the returned array may be less than the requested length if
	 * the blob ends before offset+length, and it may be more if it convenient
	 * to return more data than is requested (e.g. if the blob is backed by a byte array).
	 * If the offset given is greater than or equal to the size of the blob, this should
	 * return null, indicating that there is no more data. */
	public byte[] getData(long offset, int length);
}
