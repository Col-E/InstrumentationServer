package software.coley.instrument.util;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;

/**
 * Buffer IO util.
 *
 * @author Matt Coley
 */
public class Buffers {
	public static String getString(ByteBuffer buffer) {
		int length = buffer.getInt();
		byte[] data = new byte[length];
		buffer.get(data);
		return new String(data);
	}

	public static void putString(ByteBuffer buffer, String string) {
		buffer.putInt(string.length());
		buffer.put(string.getBytes());
	}

	/**
	 * @param channel
	 * 		Channel to read from.
	 * @param buffer
	 * 		Buffer to read into.
	 *
	 * @return Bytes read.
	 */
	public static CompletableFuture<Integer> readFrom(AsynchronousSocketChannel channel, ByteBuffer buffer) {
		// Sanity, ensure the buffer position is 0 for our use cases.
		buffer.clear();
		// Create a completable future from the completion handler provided by AsynchronousSocketChannel.
		CompletableFuture<Integer> completableFuture = new CompletableFuture<>();
		channel.read(buffer, null, new CompletionHandler<Integer, Void>() {
			@Override
			public void completed(Integer result, Void attachment) {
				// After reading into the buffer, set the limit to the current position and the position to the start.
				// This prepares for 'get' call usage while preventing overflowing excess 'get' calls with the new limit.
				buffer.limit(buffer.position());
				buffer.position(0);
				completableFuture.complete(result);
			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				completableFuture.completeExceptionally(exc);
			}
		});
		return completableFuture;
	}

	/**
	 * @param channel
	 * 		Channel to write to.
	 * @param content
	 * 		Content to write.
	 *
	 * @return Bytes written.
	 */
	public static CompletableFuture<Integer> writeTo(AsynchronousSocketChannel channel, byte[] content) {
		return writeTo(channel, ByteBuffer.wrap(content));
	}

	/**
	 * @param channel
	 * 		Channel to write to.
	 * @param buffer
	 * 		Buffer to write contents of.
	 * @param content
	 * 		Content to assign to buffer. Length must be less than buffer capacity.
	 *
	 * @return Bytes written.
	 */
	public static CompletableFuture<Integer> writeTo(AsynchronousSocketChannel channel, ByteBuffer buffer, byte[] content) {
		// Reset limit/position.
		buffer.clear();
		// Put content into buffer, set limit to content length.
		buffer.put(content);
		buffer.limit(buffer.position());
		// Write content to channel.
		return writeTo(channel, buffer);
	}

	/**
	 * @param channel
	 * 		Channel to write to.
	 * @param buffer
	 * 		Buffer to write contents of.
	 *
	 * @return Bytes written.
	 */
	private static CompletableFuture<Integer> writeTo(AsynchronousSocketChannel channel, ByteBuffer buffer) {
		// Sanity, in our use case calls to this method imply the entire buffer (up to the limit) is to be written.
		buffer.position(0);
		// Create a completable future from the completion handler provided by AsynchronousSocketChannel.
		CompletableFuture<Integer> completableFuture = new CompletableFuture<>();
		channel.write(buffer, null, new CompletionHandler<Integer, Void>() {
			@Override
			public void completed(Integer result, Void attachment) {
				// Set position to 0, set limit to capacity.
				// Prepares the buffer for reading usage.
				buffer.clear();
				completableFuture.complete(result);
			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				completableFuture.completeExceptionally(exc);
			}
		});
		return completableFuture;
	}
}
