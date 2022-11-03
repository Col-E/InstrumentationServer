package software.coley.instrument.sock;

import software.coley.instrument.ApiConstants;
import software.coley.instrument.io.ByteBufferAllocator;
import software.coley.instrument.io.ByteBufferDataInput;
import software.coley.instrument.io.ByteBufferDataOutput;
import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.AbstractMessage;
import software.coley.instrument.message.MessageFactory;
import software.coley.instrument.message.broadcast.AbstractBroadcastMessage;
import software.coley.instrument.util.Logger;
import software.coley.instrument.util.NamedThreadFactory;

import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * Common channel handler for client/server communications.
 * Operates on three threads:
 * <ul>
 *     <li>{@code READ} - Thread dedicated to reading messages</li>
 *     <li>{@code WRITE} - Thread dedicated to writing messages</li>
 *     <li>{@code EVENT} - Thread dedicated to handling listener logic</li>
 * </ul>
 * Any call to {@link #write(AbstractMessage, int)} queues a message on the {@code WRITE} thread.
 * <br>
 * All reading is done on-loop in the {@code READ} thread.
 * <br>
 * Any handling of read or written messages queues an action on the {@code EVENT} thread.
 *
 * @author Matt Coley
 * @author xxDark
 */
public class ChannelHandler {
	private static final int HEADER_SIZE = 10;
	private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
	private final ExecutorService eventTaskRunner = Executors.newCachedThreadPool(new NamedThreadFactory("EVENT"));
	private final BlockingQueue<WriteResult<?>> writeQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<Runnable> eventQueue = new LinkedBlockingQueue<>();
	private final ByteChannel channel;
	private final ByteBufferAllocator allocator;
	private final MessageFactory factory;
	private final Map<Integer, ResponseListener> responseListeners = new ConcurrentHashMap<>();
	private final AtomicInteger nextFrameId = new AtomicInteger(0);
	private ResponseListener allResponsesListener;
	private BroadcastListener broadcastListener;
	private WriteListener writeListener;
	private Future<?> readLoopFuture;
	private Future<?> writeLoopFuture;
	private Future<?> eventLoopFuture;
	private boolean running;

	/**
	 * @param channel
	 * 		Wrapped channel.
	 * @param allocator
	 * 		Buffer allocator.
	 * @param factory
	 * 		Message factory configured with supported message types.
	 */
	public ChannelHandler(ByteChannel channel, ByteBufferAllocator allocator, MessageFactory factory) {
		this.channel = channel;
		this.allocator = allocator;
		this.factory = factory;
	}

	/**
	 * Start the handling threads.
	 */
	public void start() {
		if (!running) {
			running = true;
			readLoopFuture = newSingleThreadExecutor(new NamedThreadFactory("READ")).submit(this::readLoop);
			writeLoopFuture = newSingleThreadExecutor(new NamedThreadFactory("WRITE")).submit(this::writeLoop);
			eventLoopFuture = newSingleThreadExecutor(new NamedThreadFactory("EVENT-LOOP")).submit(this::eventLoop);
		}
	}

	/**
	 * Stop the handling threads.
	 */
	public void shutdown() {
		if (running) {
			running = false;
			eventQueue.clear();
			writeQueue.clear();
			readLoopFuture.cancel(true);
			writeLoopFuture.cancel(true);
			eventLoopFuture.cancel(true);
		}
	}

	/**
	 * @param value
	 * 		Value to write.
	 * @param frameId
	 * 		ID of message.
	 * @param <T>
	 * 		Type of message.
	 *
	 * @return Result data wrapper.
	 */
	public <T extends AbstractMessage> WriteResult<T> write(T value, int frameId) {
		MessageFactory.MessageInfo info = factory.getInfo(value);
		WriteResult<T> writeResult = new WriteResult<>(info.getCodec(), frameId, info.getId(), value);
		writeQueue.add(writeResult);
		return writeResult;
	}

	/**
	 * Event handling <i>(from listeners)</i> is handled on its own thread as to not disrupt the originating thread.
	 */
	private void eventLoop() {
		while (running) {
			try {
				Runnable take = eventQueue.take();
				Logger.debug("Channel event fire");
				eventTaskRunner.submit(take);
			} catch (InterruptedException ignored) {
				// Allowed
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	/**
	 * Read handling on its own thread to ensure single-access to channel reads.
	 */
	private void readLoop() {
		try {
			// Header:
			// - 4-int:   frameId
			// - 2-short: messageType
			// - 4-int:   messageLength
			// - ...    [ message data ]
			ByteBuffer headerBuffer = allocator.allocate(HEADER_SIZE);
			ByteBuffer contentBuffer;
			while (running) {
				// Read next message header
				while (headerBuffer.position() < HEADER_SIZE)
					channel.read(headerBuffer);
				headerBuffer.position(0);
				int readFrameId = headerBuffer.getInt();
				int messageType = headerBuffer.getShort();
				int messageLength = headerBuffer.getInt();
				Logger.debug("Channel read-header: " +
						"id=" + readFrameId + ", type=" + messageType + ", length=" + messageLength);
				headerBuffer.clear();
				// Read message content
				contentBuffer = (messageLength > 0) ? ByteBuffer.allocate(messageLength) : EMPTY_BUFFER;
				while (contentBuffer.position() < messageLength) {
					int reads = channel.read(contentBuffer);
					if (reads == -1)
						throw new ClosedChannelException();
				}
				contentBuffer.position(0);
				MessageFactory.MessageInfo info = factory.getInfo(messageType);
				StructureCodec<AbstractMessage> decoder = info.getCodec();
				AbstractMessage value = decoder.decode(new ByteBufferDataInput(contentBuffer));
				Logger.debug("Channel read-body: " + value);
				// Notify listeners
				if (readFrameId == ApiConstants.BROADCAST_MESSAGE_ID) {
					if (broadcastListener != null && !eventQueue.offer(() -> broadcastListener.onReceive(messageType, (AbstractBroadcastMessage) value)))
						Logger.warn("Cannot post-event of read-completion[broadcast], event-queue is full");
				} else {
					ResponseListener responseListener = responseListeners.remove(readFrameId);
					if (responseListener != null && !eventQueue.offer(() -> responseListener.onReceive(readFrameId, value)))
						Logger.warn("Cannot post-event of read-completion[response], event-queue is full");
					if (allResponsesListener != null && !eventQueue.offer(() -> allResponsesListener.onReceive(readFrameId, value)))
						Logger.warn("Cannot post-event of read-completion[all-response], event-queue is full");
				}
			}
		} catch (Throwable t) {
			// Likely caused because shutdown occurred, can ignore.
			if (!running)
				return;
			// Unknown error, log and close server.
			t.printStackTrace();
			shutdown();
		}
	}

	/**
	 * Write handling on its own thread to ensure single-access to channel writes.
	 */
	private void writeLoop() {
		try {
			ByteBufferDataOutput output = new ByteBufferDataOutput(allocator);
			while (running) {
				// Get next write operation
				WriteResult<?> write = writeQueue.take();
				// Write header to buffer
				int writeFrameId = write.getFrameId();
				Logger.debug("Channel write-header: " +
						"id=" + writeFrameId + ", type=" + write.getDecoderKey() + ", value=" + write.getValue());
				output.reset();
				write.writeHeader(output);
				// Write content to buffer
				int contentStart = output.getBuffer().position();
				write.writeTo(output);
				int contentEnd = output.getBuffer().position();
				ByteBuffer buffer = output.consume();
				// Update header's "length" value
				int contentLength = contentEnd - contentStart;
				buffer.putInt(HEADER_SIZE - 4, contentLength);
				// Write buffer to channel
				while (buffer.position() < buffer.limit())
					channel.write(buffer);
				write.complete();
				Logger.debug("Channel write-body: " +
						"length=" + contentLength);
				// Notify listener
				if (writeListener != null && !eventQueue.offer(() -> writeListener.onWrite(writeFrameId, write.getValue())))
					Logger.warn("Cannot post-event of write-completion, event-queue is full");
			}
		} catch (InterruptedException ignored) {
			// Allowed
		} catch (Throwable t) {
			// Likely caused because shutdown occurred, can ignore.
			if (!running)
				return;
			// Unknown error, log and close server.
			t.printStackTrace();
		}
	}

	/**
	 * @return Incrementing value.
	 */
	public int getNextFrameId() {
		return nextFrameId.getAndIncrement();
	}

	/**
	 * @param broadcastListener
	 * 		Listener to handle {@link AbstractBroadcastMessage} messages.
	 */
	public void setBroadcastListener(BroadcastListener broadcastListener) {
		this.broadcastListener = broadcastListener;
	}

	/**
	 * @param writeListener
	 * 		Listener to handle all write calls.
	 */
	public void setWriteListener(WriteListener writeListener) {
		this.writeListener = writeListener;
	}

	/**
	 * @param frameId
	 * 		Message id.
	 * @param listener
	 * 		Listener to handle a response with the given ID. Used once then discarded.
	 */
	public void addResponseListener(int frameId, ResponseListener listener) {
		responseListeners.put(frameId, listener);
	}

	/**
	 * @param allResponsesListener
	 * 		Listener to handle all read calls.
	 */
	public void setAllResponsesListener(ResponseListener allResponsesListener) {
		this.allResponsesListener = allResponsesListener;
	}
}
