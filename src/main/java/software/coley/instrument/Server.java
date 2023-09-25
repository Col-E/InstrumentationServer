package software.coley.instrument;

import software.coley.instrument.data.MemberData;
import software.coley.instrument.data.ThreadData;
import software.coley.instrument.io.ByteBufferAllocator;
import software.coley.instrument.message.AbstractMessage;
import software.coley.instrument.message.MessageFactory;
import software.coley.instrument.message.broadcast.AbstractBroadcastMessage;
import software.coley.instrument.message.reply.*;
import software.coley.instrument.message.request.*;
import software.coley.instrument.sock.ChannelHandler;
import software.coley.instrument.util.Discovery;
import software.coley.instrument.util.Logger;
import software.coley.instrument.util.NamedThreadFactory;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Server which exposes capabilities of {@link Instrumentation} to a client.
 *
 * @author xxDark
 * @author Matt Coley
 */
public class Server {
	public static final int DEFAULT_PORT = 25252;
	private final Set<ChannelHandler> clients = Collections.synchronizedSet(new HashSet<>());
	private final Map<Class<?>, ReplyHandler<?>> replyHandlerMap = new IdentityHashMap<>();
	private final AtomicBoolean closed = new AtomicBoolean();
	private final ServerSocketChannel serverChannel;
	private final InstrumentationHelper instrumentation;
	private final ByteBufferAllocator allocator;
	private final MessageFactory factory;
	private final int port;

	/**
	 * @param instrumentation
	 * 		Instrumentation instance.
	 * @param address
	 * 		Address to open server on.
	 * @param allocator
	 * 		Allocator instance to pass to {@link #clients client channels}.
	 * @param factory
	 * 		Message factory configured with supported message types.
	 */
	private Server(Instrumentation instrumentation, InetSocketAddress address,
				   ByteBufferAllocator allocator, MessageFactory factory) throws IOException {
		Logger.info("Opening server on: " + address);
		this.serverChannel = ServerSocketChannel.open().bind(address);
		this.instrumentation = new InstrumentationHelper(this, instrumentation);
		this.allocator = allocator;
		this.factory = factory;
		this.port = address.getPort();
		Discovery.setupDiscovery(port);
	}

	/**
	 * @param instrumentation
	 * 		Instrumentation instance.
	 * @param address
	 * 		Address to bind to.
	 * @param allocator
	 * 		Allocator instance to pass to {@link #clients client channels}.
	 * @param factory
	 * 		Message factory configured with supported message types.
	 *
	 * @return New server instance.
	 *
	 * @throws IOException
	 * 		When the {@link AsynchronousServerSocketChannel} cannot be opened on the given address.
	 */
	public static Server open(Instrumentation instrumentation, InetSocketAddress address,
							  ByteBufferAllocator allocator, MessageFactory factory) throws IOException {
		Server server = new Server(instrumentation, address, allocator, factory);
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
	public Set<ChannelHandler> getClients() {
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
			Discovery.removeDiscovery(port);
			synchronized (clients) {
				for (ChannelHandler ch : clients) {
					ch.shutdown();
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
	 * @param message
	 * 		Message to broadcast.
	 */
	public void broadcast(AbstractBroadcastMessage message) {
		for (ChannelHandler client : clients) {
			client.write(message, ApiConstants.BROADCAST_MESSAGE_ID);
		}
	}

	/**
	 * Handles data loop for each new client connection.
	 */
	private void acceptLoop() {
		Executors.newSingleThreadExecutor(new NamedThreadFactory(ChannelHandler.threadNameClientAccept)).submit(() -> {
			try {
				while (!isClosed()) {
					SocketChannel accept = serverChannel.accept();
					ChannelHandler ch = new ChannelHandler(accept, allocator, factory);
					configureChannel(ch);
					synchronized (clients) {
						Logger.info("New client: " + accept.toString());
						clients.add(ch);
						ch.start();
					}
				}
				Logger.info("Accept loop ending, socket is closed");
			} catch (IOException ex) {
				Logger.error("Server accept-loop failure: " + ex);
				close();
			}
		});
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void configureChannel(ChannelHandler ch) {
		// Setup general reply handler
		ch.setAllResponsesListener((frameId, value) -> {
			if (frameId != ApiConstants.BROADCAST_MESSAGE_ID) {
				Logger.debug("Server handling request[id=" + frameId + ", value=" + value + "]");
				ReplyHandler handler = replyHandlerMap.get(value.getClass());
				if (handler != null)
					handler.accept(frameId, value);
			}
		});
		// Setup response handling
		InstrumentationHelper inst = instrumentation;
		answer(ch, RequestPingMessage.class, ReplyPingMessage::new);
		answer(ch, RequestThreadsMessage.class, () -> new ReplyThreadsMessage(Thread.getAllStackTraces().keySet().stream()
				.map(ThreadData::new)
				.collect(Collectors.toList())));
		answer(ch, RequestPropertiesMessage.class, () -> new ReplyPropertiesMessage(System.getProperties()));
		answer(ch, RequestSetPropertyMessage.class, req -> {
			System.getProperties().put(req.getKey(), req.getValue());
			return new ReplySetPropertyMessage();
		});
		answer(ch, RequestClassloadersMessage.class, () -> new ReplyClassloadersMessage(inst.getLoaders()));
		answer(ch, RequestClassloaderClassesMessage.class, req -> {
			int loaderId = req.getLoaderId();
			return new ReplyClassloaderClassesMessage(loaderId, inst.getLoaderClasses(loaderId));
		});
		answer(ch, RequestClassMessage.class, req ->
				new ReplyClassMessage(inst.getClassData(req.getLoaderId(), req.getName())));
		answer(ch, RequestRedefineMessage.class, req -> {
			inst.lock();
			try {
				String message = inst.redefineClass(req.getLoaderId(), req.getClassName(), req.getBytecode());
				if (message == null)
					return new ReplyRedefineMessage(ReplyRedefineMessage.MESSAGE_SUCCESS);
				else
					return new ReplyRedefineMessage(message);
			} catch (Exception ex) {
				return new ReplyRedefineMessage(ex);
			} finally {
				inst.unlock();
			}
		});
		answer(ch, RequestFieldGetMessage.class, req -> {
			MemberData member = req.getMemberInfo();
			try {
				return new ReplyFieldGetMessage(member, req.lookupValue());
			} catch (Exception ex) {
				return new ReplyFieldGetMessage(member, null);
			}
		});
		answer(ch, RequestFieldSetMessage.class, req -> {
			try {
				req.assignValue();
				return new ReplyFieldSetMessage(ReplyFieldSetMessage.MESSAGE_SUCCESS);
			} catch (Exception ex) {
				return new ReplyFieldSetMessage(ex.toString());
			}
		});
	}

	private <T extends AbstractMessage, R extends AbstractMessage>
	void answer(ChannelHandler ch, Class<T> type, Function<? super T, R> fn) {
		addHandler(type, (frameId, value) -> ch.write(fn.apply(value), frameId));
	}

	private <T extends AbstractMessage, R extends AbstractMessage>
	void answer(ChannelHandler ch, Class<T> type, Supplier<R> fn) {
		addHandler(type, (frameId, value) -> ch.write(fn.get(), frameId));
	}

	private <T extends AbstractMessage> void addHandler(Class<T> type, ReplyHandler<T> handler) {
		replyHandlerMap.put(type, handler);
	}

	private interface ReplyHandler<T extends AbstractMessage> {
		void accept(int frameId, T value);
	}
}