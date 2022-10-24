package software.coley.instrument.sock;

import software.coley.instrument.InstrumentationHelper;
import software.coley.instrument.Server;
import software.coley.instrument.message.reply.*;
import software.coley.instrument.message.request.*;
import software.coley.instrument.data.MemberData;
import software.coley.instrument.io.ByteBufferAllocator;
import software.coley.instrument.util.Logger;

import java.nio.channels.AsynchronousByteChannel;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Wrapper for {@link Server}'s {@link java.nio.channels.AsynchronousServerSocketChannel}.
 *
 * @author xxDark
 * @author Matt Coley
 */
public final class ServerChannelWrapper extends ChannelWrapper {
	private final Map<Class<?>, ReplyHandler<?>> handlerMap = new IdentityHashMap<>();
	private final Server server;

	/**
	 * @param channel
	 * 		Wrapped channel.
	 * @param allocator
	 * 		Allocator for creating messages.
	 * @param server
	 * 		Server instance.
	 */
	public ServerChannelWrapper(AsynchronousByteChannel channel, ByteBufferAllocator allocator, Server server) {
		super(channel, allocator);
		this.server = server;
		setup();
		readLoop();
	}

	@Override
	protected void onClose() {
		server.getClients().remove(this);
	}

	/**
	 * Handles reading loop for taking in new values from the connected client.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private void readLoop() {
		read().thenCompose(readResult -> {
			Object value = readResult.getValue();
			Logger.debug("Channel read[id=" + readResult.getFrameId() + "] completion: " + value);
			ReplyHandler handler = handlerMap.get(value.getClass());
			return handler.accept(readResult).getFuture();
		}).thenRun(this::readLoop);
	}

	/**
	 * Registers message responses.
	 */
	private void setup() {
		InstrumentationHelper inst = server.getInstrumentation();
		answer(RequestPingMessage.class, ReplyPingMessage::new);
		answer(RequestPropertiesMessage.class, () -> new ReplyPropertiesMessage(System.getProperties()));
		answer(RequestSetPropertyMessage.class, req -> {
			System.getProperties().put(req.getKey(), req.getValue());
			return new ReplySetPropertyMessage();
		});
		answer(RequestClassloadersMessage.class, () -> new ReplyClassloadersMessage(inst.getLoaders()));
		answer(RequestClassloaderClassesMessage.class, req -> {
			int loaderId = req.getLoaderId();
			return new ReplyClassloaderClassesMessage(loaderId, inst.getLoaderClasses(loaderId));
		});
		answer(RequestClassMessage.class, req ->
				new ReplyClassMessage(inst.getClassData(req.getLoaderId(), req.getName())));
		answer(RequestRedefineMessage.class, req -> {
			inst.lock();
			try {
				inst.redefineClass(req.getClassName(), req.getBytecode());
				return new ReplyRedefineMessage(ReplyRedefineMessage.MESSAGE_SUCCESS);
			} catch (Exception ex) {
				return new ReplyRedefineMessage(ex);
			} finally {
				inst.unlock();
			}
		});
		answer(RequestFieldGetMessage.class, req -> {
			MemberData member = req.getMemberInfo();
			try {
				return new ReplyFieldGetMessage(member, req.lookupValue());
			} catch (Exception ex) {
				return new ReplyFieldGetMessage(member, null);
			}
		});
		answer(RequestFieldSetMessage.class, req -> {
			try {
				req.assignValue();
				return new ReplyFieldSetMessage(ReplyFieldSetMessage.MESSAGE_SUCCESS);
			} catch (Exception ex) {
				return new ReplyFieldSetMessage(ex.toString());
			}
		});
	}

	private <T> void answer(Class<T> type, Function<? super T, ?> fn) {
		addHandler(type, readResult -> write(fn.apply(readResult.getValue()), readResult.getFrameId()));
	}

	private <T> void answer(Class<T> type, Supplier<?> fn) {
		addHandler(type, readResult -> write(fn.get(), readResult.getFrameId()));
	}

	private <T> void addHandler(Class<T> type, ReplyHandler<T> handler) {
		handlerMap.put(type, handler);
	}

	private interface ReplyHandler<T> {
		WriteResult accept(ReadResult<T> message);
	}
}
