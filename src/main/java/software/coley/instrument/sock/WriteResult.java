package software.coley.instrument.sock;

import java.util.concurrent.CompletableFuture;

/**
 * Wrapper for write information.
 *
 * @author Matt Coley
 */
public class WriteResult {
	private final CompletableFuture<Void> future;
	private final int frameId;

	/**
	 * @param future
	 * 		Write completion future.
	 * @param frameId
	 * 		ID of write operation.
	 */
	public WriteResult(CompletableFuture<Void> future, int frameId) {
		this.future = future;
		this.frameId = frameId;
	}

	/**
	 * @return Write completion future.
	 */
	public CompletableFuture<Void> getFuture() {
		return future;
	}

	/**
	 * @return ID of write operation.
	 */
	public int getFrameId() {
		return frameId;
	}
}
