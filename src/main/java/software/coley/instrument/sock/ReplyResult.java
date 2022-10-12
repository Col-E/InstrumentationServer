package software.coley.instrument.sock;

import java.util.concurrent.CompletableFuture;

/**
 * Wrapper for waiting on a response to a written request.
 *
 * @author Matt Coley
 */
public class ReplyResult {
	private final WriteResult writeResult;
	private final CompletableFuture<Object> replyFuture;

	/**
	 * @param writeResult
	 * 		Initial write request, containing the request command.
	 * @param replyFuture
	 * 		Future for waiting on a response to be acknowledged.
	 */
	public ReplyResult(WriteResult writeResult, CompletableFuture<Object> replyFuture) {
		this.writeResult = writeResult;
		this.replyFuture = replyFuture;
	}

	/**
	 * @return Initial write request, containing the request command.
	 */
	public WriteResult getWriteResult() {
		return writeResult;
	}

	/**
	 * @return Future for waiting on a response to be acknowledged.
	 */
	public CompletableFuture<Object> getReplyFuture() {
		return replyFuture;
	}
}
