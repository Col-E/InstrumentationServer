package software.coley.instrument;

import software.coley.instrument.io.ByteBufferAllocator;
import software.coley.instrument.message.AbstractMessage;
import software.coley.instrument.message.MessageConstants;
import software.coley.instrument.message.reply.AbstractReplyMessage;
import software.coley.instrument.message.request.AbstractRequestMessage;
import software.coley.instrument.sock.ClientChannelWrapper;
import software.coley.instrument.sock.ReplyResult;
import software.coley.instrument.sock.WriteResult;
import software.coley.instrument.util.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Client which talks to a server in order to do remote instrumentation work.
 *
 * @author Matt Coley
 */
public class Client {
	private final ClientChannelWrapper clientChannel;
	private final InetSocketAddress hostAddress;

	/**
	 * @param ip
	 * 		Server IP to connect to.
	 * @param port
	 * 		Port to connect on.
	 * @param allocator
	 * 		Allocator strategy to use.
	 *
	 * @throws IOException
	 * 		When the {@link AsynchronousSocketChannel} cannot be opened.
	 */
	public Client(String ip, int port, ByteBufferAllocator allocator) throws IOException {
		this.clientChannel = new ClientChannelWrapper(AsynchronousSocketChannel.open(), allocator);
		this.hostAddress = new InetSocketAddress(ip, port);
	}

	/**
	 * Connects to the target {@link #hostAddress}.
	 *
	 * @return {@code true} on successful connect.
	 */
	public boolean connect() {
		try {
			clientChannel.connect(hostAddress);
			return true;
		} catch (Exception ex) {
			Logger.error("Failed to connect to host: " + hostAddress + " - " + ex);
			return false;
		}
	}

	/**
	 * Close connection.
	 */
	public void close() {
		clientChannel.close();
	}

	/**
	 * @param message
	 * 		Message to send.
	 *
	 * @return Write completion.
	 */
	public WriteResult sendAsync(AbstractMessage message) {
		return clientChannel.write(message, clientChannel.getNextFrameId());
	}

	/**
	 * @param message
	 * 		Message to send.
	 * @param replyHandler
	 * 		Handler for replied messages.
	 *
	 * @return Reply result.
	 */
	@SuppressWarnings("unchecked")
	public <ReplyType extends AbstractReplyMessage, RequestType extends AbstractRequestMessage<ReplyType>>
	ReplyResult<ReplyType> sendAsync(RequestType message, Consumer<ReplyType> replyHandler) {
		CompletableFuture<ReplyType> replyFuture = new CompletableFuture<>();
		int frameId = clientChannel.getNextFrameId();
		clientChannel.setResponseListener(frameId, value -> {
			try {
				ReplyType reply = (ReplyType) value;
				if (replyHandler != null)
					replyHandler.accept(reply);
				replyFuture.complete(reply);
			} catch (Exception ex) {
				replyFuture.completeExceptionally(ex);
			}
		});
		WriteResult writeResult = clientChannel.write(message, frameId);
		return new ReplyResult<>(writeResult, replyFuture);
	}

	/**
	 * @param message
	 * 		Message to send.
	 */
	public synchronized void sendBlocking(AbstractMessage message) {
		String title = "sending message (without reply expected)";
		try {
			sendAsync(message).getFuture().get(MessageConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Logger.error("Client interrupted while " + title);
			close();
		} catch (ExecutionException e) {
			Logger.error("Client encountered error " + title + " into buffer: " + e.getCause());
			close();
		} catch (TimeoutException e) {
			Logger.error("Client timed out " + title);
			close();
		}
	}

	/**
	 * @param message
	 * 		Message to send.
	 * @param replyHandler
	 * 		Handler for replied messages.
	 */
	public synchronized <ReplyType extends AbstractReplyMessage, RequestType extends AbstractRequestMessage<ReplyType>>
	void sendBlocking(RequestType message, Consumer<ReplyType> replyHandler) {
		String title = "sending message (reply expected)";
		try {
			sendAsync(message, replyHandler).getReplyFuture()
					.get(MessageConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Logger.error("Client interrupted while " + title);
			close();
		} catch (ExecutionException e) {
			Logger.error("Client encountered error " + title + " into buffer: " + e.getCause());
			close();
		} catch (TimeoutException e) {
			Logger.error("Client timed out " + title);
			close();
		}
	}
}
