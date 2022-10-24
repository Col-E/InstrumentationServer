package software.coley.instrument.sock;

import software.coley.instrument.message.reply.AbstractReplyMessage;
import software.coley.instrument.message.request.AbstractRequestMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Wrapper for waiting on a response to a written request.
 *
 * @param <R>
 * 		Reply message type.
 *
 * @author Matt Coley
 */
public class ReplyResult<Q extends AbstractRequestMessage<R>, R extends AbstractReplyMessage> {
	private final WriteResult<Q> writeResult;
	private final CompletableFuture<R> replyFuture;

	/**
	 * @param writeResult
	 * 		Initial write request, containing the request message.
	 * @param replyFuture
	 * 		Future for waiting on a response to be acknowledged.
	 */
	public ReplyResult(WriteResult<Q> writeResult, CompletableFuture<R> replyFuture) {
		this.writeResult = writeResult;
		this.replyFuture = replyFuture;
	}

	/**
	 * @return Initial write request, containing the request message.
	 */
	public WriteResult<Q> getWriteResult() {
		return writeResult;
	}

	/**
	 * @return Future for waiting on a response to be acknowledged.
	 */
	public CompletableFuture<R> getReplyFuture() {
		return replyFuture;
	}
}
