package software.coley.instrument;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.command.CommandConstants;
import software.coley.instrument.command.CommandFactory;
import software.coley.instrument.util.Buffers;
import software.coley.instrument.util.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Client which talks to a server in order to do remote instrumentation work.
 *
 * @author Matt Coley
 */
public class Client {
	private final ExecutorService service = Executors.newSingleThreadExecutor();
	private final ByteBuffer headerBuffer = ByteBuffer.allocate(CommandConstants.HEADER_SIZE);
	private final AsynchronousSocketChannel clientChannel;
	private final InetSocketAddress hostAddress;

	/**
	 * New localhost client on default port.
	 *
	 * @throws IOException
	 * 		When the {@link AsynchronousSocketChannel} cannot be opened.
	 */
	public Client() throws IOException {
		this("localhost", Server.DEFAULT_PORT);
	}

	/**
	 * New localhost client on the given port.
	 *
	 * @param port
	 * 		Port to connect on.
	 *
	 * @throws IOException
	 * 		When the {@link AsynchronousSocketChannel} cannot be opened.
	 */
	public Client(int port) throws IOException {
		this("localhost", port);
	}

	/**
	 * New client on for the ip on the default port.
	 *
	 * @param ip
	 * 		Server IP to connect to.
	 *
	 * @throws IOException
	 * 		When the {@link AsynchronousSocketChannel} cannot be opened.
	 */
	public Client(String ip) throws IOException {
		this(ip, Server.DEFAULT_PORT);
	}

	/**
	 * @param ip
	 * 		Server IP to connect to.
	 * @param port
	 * 		Port to connect on.
	 *
	 * @throws IOException
	 * 		When the {@link AsynchronousSocketChannel} cannot be opened.
	 */
	public Client(String ip, int port) throws IOException {
		this.clientChannel = AsynchronousSocketChannel.open();
		this.hostAddress = new InetSocketAddress(ip, port);
	}

	/**
	 * Connects to the target {@link #hostAddress}.
	 *
	 * @return Connection future.
	 */
	public Future<Void> connect() {
		return clientChannel.connect(hostAddress);
	}

	/**
	 * Close connection.
	 */
	public void close() {
		try {
			if (clientChannel.isOpen())
				clientChannel.close();
		} catch (IOException ex) {
			Logger.error("Failed to close client connection to server: " + ex);
		}
	}

	/**
	 * @param command
	 * 		Command to handle.
	 */
	private void handleCommand(AbstractCommand command) {
		// TODO: handle
	}

	/**
	 * @param command
	 * 		Command to send.
	 * @param replyHandler
	 * 		Handler for replied packets.
	 *
	 * @return Number of replies.
	 */
	public Future<Integer> sendAsync(AbstractCommand command, Consumer<AbstractCommand> replyHandler) {
		return service.submit(() -> sendBlocking(command, replyHandler).get());
	}

	/**
	 * @param command
	 * 		Command to send.
	 * @param replyHandler
	 * 		Handler for replied packets.
	 *
	 * @return Number of replies.
	 */
	public Future<Integer> sendBlocking(AbstractCommand command, Consumer<AbstractCommand> replyHandler) {
		byte[] data = command.generate();
		Logger.debug("Client sending command: " + command.getClass().getSimpleName() +
				"[key=" + command.key() + ", size=" + data.length + "]");
		// Wrap bytes of command, send to channel.
		try {
			if (clientChannel.isOpen()) {
				Buffers.writeTo(clientChannel, data)
						.get(CommandConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
			} else {
				Logger.error("Client cannot write command, channel is closed");
				return CompletableFuture.completedFuture(0);
			}
		} catch (InterruptedException e) {
			Logger.error("Client interrupted while writing command data");
			close();
			return CompletableFuture.completedFuture(0);
		} catch (ExecutionException e) {
			Logger.error("Client encountered error writing command data into buffer");
			close();
			return CompletableFuture.completedFuture(0);
		} catch (TimeoutException e) {
			Logger.error("Client timed out writing command data");
			close();
			return CompletableFuture.completedFuture(0);
		}
		// Handle reply from channel.
		int replies = 0;
		while (true) {
			// Read response into buffer
			try {
				Logger.debug("Client awaiting server response...");
				Buffers.readFrom(clientChannel, headerBuffer)
						.get(CommandConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
				replies++;
			} catch (InterruptedException e) {
				Logger.error("Client interrupted while reading command reply");
				close();
				return CompletableFuture.completedFuture(replies);
			} catch (ExecutionException e) {
				Logger.error("Client encountered error reading command reply into buffer");
				close();
				return CompletableFuture.completedFuture(replies);
			} catch (TimeoutException e) {
				Logger.error("Client timed out reading command reply");
				close();
				return CompletableFuture.completedFuture(replies);
			}
			// Read header from headerBuffer
			byte commandId = headerBuffer.get();
			int commandLength = headerBuffer.getInt();
			// Check if reply was 'DONE'
			if (commandId == CommandConstants.HEADER_PART_DONE && commandLength == -1) {
				Logger.debug("Client received 'DONE' from server");
				break;
			}
			// Parse and handle command
			AbstractCommand reply = CommandFactory.create(commandId);
			if (reply == null) {
				Logger.error("Client read reply from server, unknown command: " + commandId);
			} else {
				Logger.debug("Client read from server, command: " +
						reply.getClass().getSimpleName() + "[" + commandId + "]");
				// Allocate new headerBuffer and read into it the remaining data
				if (commandLength > 0) {
					try {
						ByteBuffer commandDataBuffer = ByteBuffer.allocate(commandLength);
						Buffers.readFrom(clientChannel, commandDataBuffer)
								.get(CommandConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
						reply.read(commandDataBuffer);
					} catch (InterruptedException e) {
						Logger.error("Client interrupted while reading remaining command data");
						close();
						return CompletableFuture.completedFuture(replies);
					} catch (ExecutionException ex) {
						Logger.error("Client encountered error reading remaining command data into headerBuffer");
						close();
						return CompletableFuture.completedFuture(replies);
					} catch (TimeoutException ex) {
						Logger.error("Client timed out reading remaining command data");
						close();
						return CompletableFuture.completedFuture(replies);
					}
				}
				// Handle parsed command data.
				replyHandler.accept(reply);
			}
		}
		return CompletableFuture.completedFuture(replies);
	}
}
