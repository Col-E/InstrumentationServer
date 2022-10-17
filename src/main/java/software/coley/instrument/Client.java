package software.coley.instrument;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.command.CommandConstants;
import software.coley.instrument.io.ByteBufferAllocator;
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
	 * @param command
	 * 		Command to send.
	 *
	 * @return Write completion.
	 */
	public WriteResult sendAsync(AbstractCommand command) {
		return clientChannel.write(command, clientChannel.getNextFrameId());
	}

	/**
	 * @param command
	 * 		Command to send.
	 * @param replyHandler
	 * 		Handler for replied packets.
	 *
	 * @return Reply result.
	 */
	@SuppressWarnings("unchecked")
	public <R extends AbstractCommand> ReplyResult sendAsync(AbstractCommand command, Consumer<R> replyHandler) {
		CompletableFuture<Object> replyFuture = new CompletableFuture<>();
		int frameId = clientChannel.getNextFrameId();
		clientChannel.setResponseListener(frameId, value -> {
			try {
				if (replyHandler != null)
					replyHandler.accept((R) value);
				replyFuture.complete(value);
			} catch (Exception ex) {
				replyFuture.completeExceptionally(ex);
			}
		});
		WriteResult writeResult = clientChannel.write(command, frameId);
		return new ReplyResult(writeResult, replyFuture);
	}

	/**
	 * @param command
	 * 		Command to send.
	 */
	public synchronized void sendBlocking(AbstractCommand command) {
		String title = "sending command (without reply expected)";
		try {
			sendAsync(command).getFuture().get(CommandConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
	 * @param command
	 * 		Command to send.
	 * @param replyHandler
	 * 		Handler for replied packets.
	 */
	@SuppressWarnings("unchecked")
	public synchronized <R extends AbstractCommand> void sendBlocking(AbstractCommand command, Consumer<R> replyHandler) {
		String title = "sending command (reply expected)";
		try {
			sendAsync(command, (Consumer<AbstractCommand>) replyHandler).getReplyFuture()
					.get(CommandConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
