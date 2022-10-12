package software.coley.instrument;

import software.coley.instrument.io.ByteBufferAllocator;
import software.coley.instrument.sock.ChannelWrapper;
import software.coley.instrument.sock.ServerChannelWrapper;
import software.coley.instrument.util.Logger;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server which exposes capabilities of {@link Instrumentation} to a client.
 *
 * @author xxDark
 * @author Matt Coley
 */
public class Server {
	public static final int DEFAULT_PORT = 25252;
	private final Set<ChannelWrapper> clients = Collections.synchronizedSet(new HashSet<>());
	private final AtomicBoolean closed = new AtomicBoolean();
	private final AsynchronousServerSocketChannel serverChannel;
	private final InstrumentationHelper instrumentation;
	private final ByteBufferAllocator allocator;

	/**
	 * @param instrumentation
	 * 		Instrumentation instance.
	 * @param serverChannel
	 * 		Channel to operate on.
	 * @param allocator
	 * 		Allocator instance to pass to {@link #clients client channels}.
	 */
	private Server(Instrumentation instrumentation, AsynchronousServerSocketChannel serverChannel,
				   ByteBufferAllocator allocator) {
		this.instrumentation = new InstrumentationHelper(instrumentation);
		this.serverChannel = serverChannel;
		this.allocator = allocator;
	}

	/**
	 * @param instrumentation
	 * 		Instrumentation instance.
	 * @param address
	 * 		Address to bind to.
	 * @param allocator
	 * 		Allocator instance to pass to {@link #clients client channels}.
	 *
	 * @return New server instance.
	 *
	 * @throws IOException
	 * 		When the {@link AsynchronousServerSocketChannel} cannot be opened on the given address.
	 */
	public static Server open(Instrumentation instrumentation, InetSocketAddress address,
							  ByteBufferAllocator allocator) throws IOException {
		Logger.info("Opening server on: " + address);
		AsynchronousServerSocketChannel ch = AsynchronousServerSocketChannel.open().bind(address);
		Server server = new Server(instrumentation, ch, allocator);
		server.acceptLoop();
		return server;
	}

	/**
	 * @return Wrapped instrumentation instance.
	 */
	public InstrumentationHelper getInstrumentation() {
		return instrumentation;
	}

	/**
	 * @return Active clients.
	 */
	public Set<ChannelWrapper> getClients() {
		return clients;
	}

	/**
	 * @return {@code true} if server is no longer active.
	 */
	public boolean isClosed() {
		return closed.get();
	}

	/**
	 * Closes the server.
	 */
	public void close() {
		if (closed.compareAndSet(false, true)) {
			Logger.debug("Closing client connections");
			synchronized (clients) {
				for (ChannelWrapper ch : clients) {
					ch.close();
				}
			}
			try {
				serverChannel.close();
			} catch (IOException ignored) {
			}
			Logger.info("Server closed");
		} else {
			Logger.debug("Server already closed");
		}
	}

	/**
	 * Handles data loop for each new client connection.
	 */
	private void acceptLoop() {
		serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
			@Override
			public void completed(AsynchronousSocketChannel result, Object attachment) {
				ServerChannelWrapper ch = new ServerChannelWrapper(result, allocator, Server.this);
				synchronized (clients) {
					clients.add(ch);
				}
				acceptLoop();
			}

			@Override
			public void failed(Throwable ex, Object attachment) {
				Logger.error("Server accept-loop failure: " + ex);
				close();
			}
		});
	}
}
