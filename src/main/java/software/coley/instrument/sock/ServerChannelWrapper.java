package software.coley.instrument.sock;

import software.coley.instrument.InstrumentationHelper;
import software.coley.instrument.Server;
import software.coley.instrument.command.reply.*;
import software.coley.instrument.command.request.*;
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
	private final Map<Class<?>, CommandHandler<?>> handlerMap = new IdentityHashMap<>();
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
			CommandHandler handler = handlerMap.get(value.getClass());
			return handler.accept(readResult).getFuture();
		}).thenRun(this::readLoop);
	}

	/**
	 * Registers packet responses.
	 */
	private void setup() {
		InstrumentationHelper inst = server.getInstrumentation();
		answer(RequestPingCommand.class, ReplyPingCommand::new);
		answer(RequestPropertiesCommand.class, () -> new ReplyPropertiesCommand(System.getProperties()));
		answer(RequestSetPropertyCommand.class, req -> {
			System.getProperties().put(req.getKey(), req.getValue());
			return new ReplySetPropertyCommand();
		});
		answer(RequestClassloadersCommand.class, () -> new ReplyClassloadersCommand(inst.getLoaders()));
		answer(RequestClassloaderClassesCommand.class, req -> {
			int loaderId = req.getLoaderId();
			return new ReplyClassloaderClassesCommand(loaderId, inst.getLoaderClasses(loaderId));
		});
		answer(RequestClassCommand.class, req -> new ReplyClassCommand(inst.getClassData(req.getName())));
		answer(RequestRedefineCommand.class, req -> {
			inst.lock();
			try {
				inst.redefineClass(req.getClassName(), req.getBytecode());
				return new ReplyRedefineCommand(ReplyRedefineCommand.MESSAGE_SUCCESS);
			} catch (Exception ex) {
				return new ReplyRedefineCommand(ex);
			} finally {
				inst.unlock();
			}
		});
		answer(RequestFieldGetCommand.class, req -> {
			MemberData member = req.getMemberInfo();
			try {
				return new ReplyFieldGetCommand(member, req.lookupValue());
			} catch (Exception ex) {
				return new ReplyFieldGetCommand(member, null);
			}
		});
		answer(RequestFieldSetCommand.class, req -> {
			try {
				req.assignValue();
				return new ReplyFieldSetCommand(ReplyFieldSetCommand.MESSAGE_SUCCESS);
			} catch (Exception ex) {
				return new ReplyFieldSetCommand(ex.toString());
			}
		});
	}

	private <T> void answer(Class<T> type, Function<? super T, ?> fn) {
		addHandler(type, readResult -> write(fn.apply(readResult.getValue()), readResult.getFrameId()));
	}

	private <T> void answer(Class<T> type, Supplier<?> fn) {
		addHandler(type, readResult -> write(fn.get(), readResult.getFrameId()));
	}

	private <T> void addHandler(Class<T> type, CommandHandler<T> handler) {
		handlerMap.put(type, handler);
	}

	private interface CommandHandler<T> {
		WriteResult accept(ReadResult<T> command);
	}
}
