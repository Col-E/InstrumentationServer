package software.coley.instrument.sock;

import software.coley.instrument.command.CommandFactory;
import software.coley.instrument.io.ByteBufferAllocator;
import software.coley.instrument.io.ByteBufferDataInput;
import software.coley.instrument.io.ByteBufferDataOutput;
import software.coley.instrument.io.ByteBufferSanitizer;
import software.coley.instrument.util.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wrapper for {@link AsynchronousByteChannel} <i>(And child types)</i> to facilitate communications with either a
 * {@link software.coley.instrument.Server} or {@link software.coley.instrument.Client}.
 *
 * @author xxDark
 */
public class ChannelWrapper {
	private final CommandFactory factory = CommandFactory.create();
	private final AtomicBoolean closed = new AtomicBoolean();
	private final AsynchronousByteChannel channel;
	private final ByteBufferSanitizer output;
	private final ByteBufferSanitizer input;

	/**
	 * @param channel
	 * 		Wrapped channel.
	 * @param allocator
	 * 		Allocator for creating messages.
	 */
	public ChannelWrapper(AsynchronousByteChannel channel, ByteBufferAllocator allocator) {
		this.channel = channel;
		output = new ByteBufferSanitizer(allocator);
		input = new ByteBufferSanitizer(allocator);
	}

	/**
	 * @param channel
	 * 		Wrapped channel.
	 */
	public ChannelWrapper(AsynchronousByteChannel channel) {
		this(channel, ByteBufferAllocator.HEAP);
	}

	/**
	 * @param value
	 * 		Value to write.
	 * 		The {@code class} type of the value must be recognized by the {@link CommandFactory}
	 *
	 * @return Future of write completion.
	 */
	public CompletableFuture<Void> write(Object value) {
		ByteBufferSanitizer output = this.output;
		output.clear();
		output.ensureWriteable(4).putInt(-1);
		try {
			factory.encode(new ByteBufferDataOutput(output), value);
		} catch (IOException ex) {
			throw new IllegalArgumentException("Failed to encode value: " + value, ex);
		}
		CompletableFuture<Void> future = new CompletableFuture<>();
		ByteBuffer result = output.consume();
		result.putInt(0, result.limit() - 4);
		channel.write(result, null, new CompletionHandler<Integer, Object>() {
			@Override
			public void completed(Integer result, Object attachment) {
				future.complete(null);
			}

			@Override
			public void failed(Throwable exc, Object attachment) {
				future.completeExceptionally(exc);
			}
		});
		return future;
	}

	/**
	 * @param <T>
	 * 		Read value type.
	 *
	 * @return Future of read value.
	 */
	public <T> CompletableFuture<T> read() {
		Logger.debug("Channel read initiate");
		ByteBufferSanitizer input = this.input;
		input.clear();
		input.ensureWriteable(4);
		CompletableFuture<T> future = new CompletableFuture<>();
		beginRead(future);
		return future;
	}

	/**
	 * Closes channel.
	 */
	public void close() {
		if (closed.compareAndSet(false, true)) {
			try {
				channel.close();
			} catch (IOException ignored) {
			}
			onClose();
			Logger.info("Closing channel");
		} else {
			Logger.info("Channel already closed");
		}
	}

	/**
	 * Implemented by children.
	 */
	protected void onClose() {
		// no-op by default
	}

	/**
	 * Handles reading from the channel, addressing potential cases where multiple reads are required.
	 *
	 * @param future
	 * 		Future to complete.
	 */
	private void beginRead(CompletableFuture<?> future) {
		// Must be already set up
		ByteBuffer buffer = input.getBuffer();
		if (startFrameRead(buffer, future)) {
			return;
		}
		Logger.debug("Channel read begin [" + buffer.limit() + "]");
		channel.read(buffer, null, new CompletionHandler<Integer, Object>() {
			@Override
			public void completed(Integer result, Object attachment) {
				if (result == -1) {
					Logger.debug("Channel read completion exceptionally: unmatched content length");
					future.completeExceptionally(new ClosedChannelException());
					close();
					return;
				}
				if (!startFrameRead(buffer, future)) {
					beginRead(future);
				}
			}

			@Override
			public void failed(Throwable ex, Object attachment) {
				Logger.debug("Channel read completion exceptionally: " + ex);
				future.completeExceptionally(ex);
			}
		});
	}

	/**
	 * Initializes a read operation for some content <i>(one item = one frame)</i>
	 *
	 * @param buffer
	 * 		Buffer to read from.
	 * @param future
	 * 		Completion future.
	 *
	 * @return {@code true} when read can continue.
	 * {@code false} when content is not readable.
	 */
	private boolean startFrameRead(ByteBuffer buffer, CompletableFuture<?> future) {
		if (buffer.position() >= 4) {
			// Read frame length.
			int frameLength = buffer.getInt(0);
			// Resize buffer to fit full frame content.
			input.ensureWriteable(frameLength);
			// Read into the buffer.
			readFrame(future, frameLength);
			return true;
		}
		return false;
	}

	/**
	 * @param future
	 * 		Completion future.
	 * @param frameLength
	 * 		Content length.
	 */
	private void readFrame(CompletableFuture<?> future, int frameLength) {
		// Must be already set up.
		ByteBuffer buffer = input.getBuffer();
		if (!consumeFrame(buffer, frameLength, future)) {
			channel.read(buffer, null, new CompletionHandler<Integer, Object>() {
				@Override
				public void completed(Integer result, Object attachment) {
					if (result == -1) {
						Logger.debug("Channel read completion exceptionally: unmatched content length");
						future.completeExceptionally(new ClosedChannelException());
						close();
						return;
					}
					if (!consumeFrame(buffer, frameLength, future)) {
						readFrame(future, frameLength);
					}
				}

				@Override
				public void failed(Throwable ex, Object attachment) {
					Logger.debug("Channel read completion exceptionally: " + ex);
					future.completeExceptionally(ex);
				}
			});
		}
	}

	/**
	 * @param buffer
	 * 		Buffer to read from.
	 * @param frameLength
	 * 		Content length.
	 * @param future
	 * 		Completion future.
	 *
	 * @return {@code true} when frame was successfully read.
	 * {@code false} when content could not be read <i>(length beyond expected content size)</i>.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private boolean consumeFrame(ByteBuffer buffer, int frameLength, CompletableFuture<?> future) {
		int currentLength = buffer.position() - 4;
		if (currentLength >= frameLength) {
			try {
				buffer.position(4); // Push position back, read full frame
				Object value = factory.decode(new ByteBufferDataInput(buffer));
				int position = buffer.position();
				int limit = buffer.limit();
				if (position != limit) {
					ByteBuffer slice = buffer.slice();
					buffer.clear();
					buffer.put(slice);
				} else {
					buffer.clear();
				}
				((CompletableFuture) future).complete(value);
			} catch (IOException ex) {
				future.completeExceptionally(ex);
				close();
			}
			return true;
		}
		return false;
	}
}
