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
	 * @return {@code true} on successful connect.
	 */
	public boolean connect() {
		try {
			clientChannel.connect(hostAddress).get();
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
		try {
			if (clientChannel.isOpen())
				clientChannel.close();
		} catch (IOException ex) {
			Logger.error("Failed to close client connection to server: " + ex);
		}
	}

	/**
	 * Place to handle commands generically, as opposed to using {@link Consumer} on message replies in
	 * {@link #send(AbstractCommand, Consumer)}.
	 *
	 * @param command
	 * 		Command to handle.
	 */
	protected void handleCommand(AbstractCommand command) {
		// no-op by default
	}

	/**
	 * @param command
	 * 		Command to send.
	 * @param replyHandler
	 * 		Handler for replied packets.
	 *
	 * @return Number of replies.
	 */
	public CompletableFuture<Integer> sendAsync(AbstractCommand command, Consumer<AbstractCommand> replyHandler) {
		return CompletableFuture.supplyAsync(() -> send(command, replyHandler), service);
	}

	/**
	 * @param command
	 * 		Command to send.
	 * @param replyHandler
	 * 		Handler for replied packets.
	 *
	 * @return Number of replies.
	 */
	public int send(AbstractCommand command, Consumer<AbstractCommand> replyHandler) {
		// Sanity check, channel must be open to send command.
		if (!clientChannel.isOpen()) {
			Logger.error("Client cannot write command, channel is closed");
			return 0;
		}
		// Build command data.
		byte[] data = command.generate();
		Logger.debug("Client sending command: " + command.getClass().getSimpleName() +
				"[key=" + command.key() + ", size=" + data.length + "]");
		// Wrap bytes of command, send to channel.
		if (!blockingAction(Buffers.writeTo(clientChannel, data), "writing command data"))
			return 0;
		// Handle reply from channel.
		int replies = 0;
		while (true) {
			// Read response into buffer
			Logger.debug("Client awaiting server response...");
			if (blockingAction(Buffers.readFrom(clientChannel, headerBuffer), "reading command reply"))
				replies++;
			else
				return replies;
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
					ByteBuffer commandDataBuffer = ByteBuffer.allocate(commandLength);
					if (blockingAction(Buffers.readFrom(clientChannel, commandDataBuffer), "reading remaining command data")) {
						reply.read(commandDataBuffer);
					} else {
						return replies;
					}
				}
				// Handle parsed command data.
				replyHandler.accept(reply);
			}
		}
		return replies;
	}

	/**
	 * Wraps the same {@code try-catch} logic to reduce duplicate bloat in {@link #send(AbstractCommand, Consumer)}.
	 *
	 * @param action
	 * 		Action of {@link Buffers#readFrom(AsynchronousSocketChannel, ByteBuffer)} or
	 *        {@link Buffers#writeTo(AsynchronousSocketChannel, ByteBuffer)}
	 * @param title
	 * 		Title of action to emit in error logging.
	 *
	 * @return {@code true} on success.
	 * {@code false} on failure.
	 */
	private boolean blockingAction(Future<?> action, String title) {
		try {
			action.get(CommandConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
			return true;
		} catch (InterruptedException e) {
			Logger.error("Client interrupted while " + title);
			close();
			return false;
		} catch (ExecutionException e) {
			Logger.error("Client encountered error " + title + " into buffer");
			close();
			return false;
		} catch (TimeoutException e) {
			Logger.error("Client timed out " + title);
			close();
			return false;
		}
	}
}
