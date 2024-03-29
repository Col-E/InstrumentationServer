package software.coley.instrument;

import software.coley.instrument.io.ByteBufferAllocator;
import software.coley.instrument.message.AbstractMessage;
import software.coley.instrument.message.MessageConstants;
import software.coley.instrument.message.MessageFactory;
import software.coley.instrument.message.reply.AbstractReplyMessage;
import software.coley.instrument.message.request.AbstractRequestMessage;
import software.coley.instrument.sock.BroadcastListener;
import software.coley.instrument.sock.ChannelHandler;
import software.coley.instrument.sock.ReplyResult;
import software.coley.instrument.sock.WriteResult;
import software.coley.instrument.util.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SocketChannel;
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
	private final InetSocketAddress hostAddress;
	private final SocketChannel socketChannel;
	private final ChannelHandler handler;
	private final String ip;
	private final int port;

	/**
	 * @param ip
	 * 		Server IP to connect to.
	 * @param port
	 * 		Port to connect on.
	 * @param allocator
	 * 		Allocator strategy to use.
	 * @param factory
	 * 		Message factory configured with supported message types.
	 *
	 * @throws IOException
	 * 		When the {@link AsynchronousSocketChannel} cannot be opened.
	 */
	public Client(String ip, int port, ByteBufferAllocator allocator, MessageFactory factory) throws IOException {
		this.socketChannel = SocketChannel.open();
		this.hostAddress = new InetSocketAddress(ip, port);
		this.handler = new ChannelHandler(socketChannel, allocator, factory, null);
		this.ip = ip;
		this.port = port;
	}

	/**
	 * @return Address of remote server.
	 */
	public InetSocketAddress getHostAddress() {
		return hostAddress;
	}

	/**
	 * @return IP of remote server.
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * @return Port of remote server.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param listener
	 * 		Listener to use.
	 *
	 * @see ChannelHandler#setBroadcastListener(BroadcastListener)
	 */
	public void setBroadcastListener(BroadcastListener listener) {
		handler.setBroadcastListener(listener);
	}

	/**
	 * Connects to the target {@link #hostAddress}.
	 *
	 * @return {@code true} on successful connect.
	 */
	public boolean connect() {
		try {
			connectThrowing();
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Connects to the target {@link #hostAddress}.
	 *
	 * @throws Exception
	 * 		When the connection cannot be made.
	 */
	public void connectThrowing() throws Exception {
		try {
			if (socketChannel.connect(hostAddress)) {
				handler.start();
				return;
			}
			throw new IOException("Could not connect to: " + hostAddress);
		} catch (Exception ex) {
			Logger.error("Failed to connect to host: " + hostAddress + " - " + ex);
			throw ex;
		}
	}

	/**
	 * Close connection.
	 *
	 * @throws IOException
	 * 		When the underlying socket-channel {@link SocketChannel#close()} throws.
	 */
	public void close() throws IOException {
		handler.shutdown();
		socketChannel.close();
	}

	/**
	 * Internal usage only.
	 * Call {@link #close()} silently.
	 */
	private void quietClose() {
		try {
			close();
		} catch (IOException ex) {
			Logger.error("Failed to close client: " + ex);
		}
	}

	/**
	 * @param message
	 * 		Message to send.
	 * @param <T>
	 * 		Message type.
	 *
	 * @return Write completion.
	 */
	public <T extends AbstractMessage> WriteResult<T> sendAsync(T message) {
		return handler.write(message, handler.getNextFrameId());
	}

	/**
	 * @param message
	 * 		Message to send.
	 * @param replyHandler
	 * 		Handler for replied messages.
	 * @param <RequestType>
	 * 		Message type of sent content.
	 * @param <ReplyType>
	 * 		Message type of response content.
	 *
	 * @return Reply result.
	 */
	@SuppressWarnings("unchecked")
	public <ReplyType extends AbstractReplyMessage, RequestType extends AbstractRequestMessage<ReplyType>>
	ReplyResult<RequestType, ReplyType> sendAsync(RequestType message, Consumer<ReplyType> replyHandler) {
		CompletableFuture<ReplyType> replyFuture = new CompletableFuture<>();
		int frameId = handler.getNextFrameId();
		handler.addResponseListener(frameId, (responseId, value) -> {
			try {
				ReplyType reply = (ReplyType) value;
				if (replyHandler != null)
					replyHandler.accept(reply);
				replyFuture.complete(reply);
			} catch (Throwable t) {
				t.printStackTrace();
				replyFuture.completeExceptionally(t);
			}
		});
		WriteResult<RequestType> writeResult = handler.write(message, frameId);
		return new ReplyResult<>(writeResult, replyFuture);
	}

	/**
	 * @param message
	 * 		Message to send.
	 */
	public synchronized void sendBlocking(AbstractMessage message) {
		String title = "sending message (without reply expected)";
		try {
			WriteResult<AbstractMessage> result = sendAsync(message);
			title = "sending message[id=" + result.getFrameId() + "] (without reply expected)";
			result.getFuture().get(MessageConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Logger.error("Client interrupted while " + title);
			quietClose();
		} catch (ExecutionException e) {
			Logger.error("Client encountered error " + title + " into buffer: " + e.getCause());
			quietClose();
		} catch (TimeoutException e) {
			Logger.error("Client timed out " + title);
			quietClose();
		}
	}

	/**
	 * @param message
	 * 		Message to send.
	 * @param replyHandler
	 * 		Handler for replied messages.
	 * @param <RequestType>
	 * 		Message type of sent content.
	 * @param <ReplyType>
	 * 		Message type of response content.
	 */
	public synchronized <ReplyType extends AbstractReplyMessage, RequestType extends AbstractRequestMessage<ReplyType>>
	void sendBlocking(RequestType message, Consumer<ReplyType> replyHandler) {
		String title = "sending message (reply expected)";
		try {
			ReplyResult<RequestType, ReplyType> result = sendAsync(message, replyHandler);
			WriteResult<RequestType> writeResult = result.getWriteResult();
			title = "sending message[id=" + writeResult.getFrameId() + "] (reply expected)";
			result.getReplyFuture()
					.get(MessageConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Logger.error("Client interrupted while " + title);
			quietClose();
		} catch (ExecutionException e) {
			Logger.error("Client encountered error " + title + " into buffer: " + e.getCause());
			quietClose();
		} catch (TimeoutException e) {
			Logger.error("Client timed out " + title);
			quietClose();
		}
	}
}