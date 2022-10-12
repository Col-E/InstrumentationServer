package software.coley.instrument.sock;

/**
 * Wrapper for read information.
 *
 * @author Matt Coley
 */
public class ReadResult<T> {
	private final int frameId;
	private final T value;

	/**
	 * @param frameId
	 * 		Frame ID for read value.
	 * @param value
	 * 		Value read.
	 */
	public ReadResult(int frameId, T value) {
		this.frameId = frameId;
		this.value = value;
	}

	/**
	 * Preceding the content in a byte-sequence is an ID.
	 * This value is used such that a {@link software.coley.instrument.Client} can send a request with an ID.
	 * Then the {@link software.coley.instrument.Server} can reply with a response value with the same ID.
	 * The client can then tell that the response is for that specific request since the ID matches.
	 *
	 * @return Frame ID for read value.
	 */
	public int getFrameId() {
		return frameId;
	}

	/**
	 * @return Value read.
	 */
	public T getValue() {
		return value;
	}
}
