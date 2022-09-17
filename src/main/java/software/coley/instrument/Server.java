package software.coley.instrument;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.command.CommandConstants;
import software.coley.instrument.command.CommandFactory;
import software.coley.instrument.command.impl.PongCommand;
import software.coley.instrument.util.Buffers;
import software.coley.instrument.util.Logger;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Server which exposes capabilities of {@link Instrumentation} to a client.
 *
 * @author Matt Coley
 */
public class Server {
	public static final int DEFAULT_PORT = 25252;
	private final List<AsynchronousSocketChannel> clients = new ArrayList<>();
	private final AsynchronousServerSocketChannel serverChannel;
	private final Instrumentation instrumentation;

	/**
	 * @param instrumentation
	 * 		Instrumentation instance.
	 * @param port
	 * 		Port to run on.
	 *
	 * @throws IOException
	 * 		When the {@link AsynchronousServerSocketChannel} cannot be opened.
	 */
	public Server(Instrumentation instrumentation, int port) throws IOException {
		this.instrumentation = instrumentation;
		serverChannel = AsynchronousServerSocketChannel.open();
		serverChannel.bind(new InetSocketAddress("localhost", port));
	}

	/**
	 * Async accept new client, closing prior connection if any.
	 *
	 * @param handler
	 * 		Callback on new client accept.
	 */
	public void acceptAsync(Consumer<AsynchronousSocketChannel> handler) {
		serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
			@Override
			public void completed(AsynchronousSocketChannel result, Object attachment) {
				// Handle new client
				if (handler != null)
					handler.accept(result);
				onNewClient(result);
			}

			@Override
			public void failed(Throwable ex, Object attachment) {
				Logger.error("Accepting client encountered failure: " + ex);
			}
		});
	}

	/**
	 * @param client
	 * 		Newly connected client, from {@link #acceptAsync(Consumer)}
	 */
	private void onNewClient(AsynchronousSocketChannel client) {
		// Update reference
		clients.add(client);
		// Subscribe to packet handling
		if (client != null && client.isOpen()) {
			loop(client);
		} else {
			Logger.error("Server received new client, but was not open!");
		}
	}

	/**
	 * @param clientChannel
	 * 		Client to interact with.
	 */
	private void loop(AsynchronousSocketChannel clientChannel) {
		ByteBuffer headerBuffer = ByteBuffer.allocate(CommandConstants.HEADER_SIZE);
		while (true) {
			// Wait for client command
			Logger.debug("Server waiting for new client command");
			try {
				headerBuffer.clear();
				Buffers.readFrom(clientChannel, headerBuffer)
						.get(CommandConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
				Logger.info("Server received header");
			} catch (InterruptedException e) {
				Logger.error("Server interrupted while reading command header");
				return;
			} catch (ExecutionException ex) {
				Logger.error("Server encountered error reading command header into headerBuffer: " + ex.getCause());
				return;
			} catch (TimeoutException e) {
				e.printStackTrace();
				return;
			}
			// Read header from headerBuffer
			headerBuffer.position(0);
			byte commandId = headerBuffer.get();
			int commandLength = headerBuffer.getInt();
			// Parse and handle command
			AbstractCommand command = CommandFactory.create(commandId);
			if (command == null) {
				Logger.error("Server read from client, unknown command: " + commandId);
			} else {
				Logger.debug("Server read from client, command: " +
						command.getClass().getSimpleName() + "[" + commandId + "]");
				// Allocate new headerBuffer and read into it the remaining data
				if (commandLength > 0) {
					try {
						ByteBuffer commandDataBuffer = ByteBuffer.allocate(commandLength);
						Buffers.readFrom(clientChannel, commandDataBuffer)
								.get(CommandConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
						command.read(commandDataBuffer);
					} catch (InterruptedException e) {
						Logger.error("Server interrupted while reading remaining command data");
						return;
					} catch (ExecutionException ex) {
						Logger.error("Server encountered error reading remaining command data into buffer: " + ex.getCause());
						return;
					} catch (TimeoutException ex) {
						Logger.error("Server timed out reading remaining command data");
						return;
					} catch (IOException ex) {
						Logger.error("Server failed to parse command data from command: " + ex);
						return;
					}
				}
				// Handle parsed command data.
				try {
					handleCommand(command, clientChannel);
				} catch (InterruptedException e) {
					Logger.error("Server interrupted while handling command");
					return;
				} catch (ExecutionException ex) {
					Logger.error("Server encountered error handling command: " + ex.getCause());
					return;
				} catch (TimeoutException ex) {
					Logger.error("Server timed out handling command");
					return;
				}
			}
			// Resubscribe this completion handler to handle the next client command.
			// Unless the client is disconnecting of course.
			if (commandId == CommandConstants.ID_COMMON_DISCONNECT) {
				Logger.debug("Server handler closing for disconnecting client: " + clientChannel);
			} else {
				// Flip into read-mode, tell client we are done replying
				Logger.debug("Server replying DONE to client");
				// Resubscribe with our reply
				try {
					Buffers.writeTo(clientChannel, headerBuffer, CommandConstants.HEADER_DONE).get();
					Logger.debug("Server replying complete");
				} catch (InterruptedException e) {
					Logger.error("Server interrupted while replying DONE");
					return;
				} catch (ExecutionException ex) {
					Logger.error("Server encountered error replying DONE: " + ex.getCause());
					return;
				}
			}
		}
	}

	/**
	 * @param command
	 * 		Command to handle.
	 * @param clientChannel
	 * 		Channel the command is from.
	 */
	private void handleCommand(AbstractCommand command, AsynchronousSocketChannel clientChannel)
			throws ExecutionException, InterruptedException, TimeoutException {
		switch (command.key()) {
			case CommandConstants.ID_COMMON_PING:
				Buffers.writeTo(clientChannel, new PongCommand().generate())
						.get(CommandConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
				break;
			case CommandConstants.ID_COMMON_SHUTDOWN:
				close();
				break;
		}
	}

	/**
	 * Closes the server.
	 */
	public void close() {
		try {
			for (AsynchronousSocketChannel client : clients) {
				closeClientChannel(client);
			}
			// Close server
			if (serverChannel.isOpen())
				serverChannel.close();
		} catch (Exception ex) {
			Logger.error("Closing server could not complete: " + ex);
		}
	}

	/**
	 * Closes connection to the client.
	 *
	 * @param clientChannel
	 * 		Client to close.
	 */
	public void closeClientChannel(AsynchronousSocketChannel clientChannel) {
		try {
			clients.remove(clientChannel);
			if (clientChannel != null && clientChannel.isOpen()) {
				clientChannel.close();
			}
		} catch (Exception ex) {
			Logger.error("Closing server could not complete: " + ex);
		}
	}
}
