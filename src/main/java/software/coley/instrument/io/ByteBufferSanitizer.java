package software.coley.instrument.io;

import java.nio.ByteBuffer;

/**
 * Wrapper of {@link ByteBuffer}.
 *
 * @author xxDark
 */
public final class ByteBufferSanitizer {
	private static final ByteBuffer EMPTY = ByteBuffer.wrap(new byte[0]);
	private final ByteBufferAllocator allocator;
	private ByteBuffer buffer = EMPTY;

	/**
	 * @param allocator
	 *        {@link ByteBuffer} allocator.
	 */
	public ByteBufferSanitizer(ByteBufferAllocator allocator) {
		this.allocator = allocator;
	}

	/**
	 * @return Underlying buffer.
	 */
	public ByteBuffer getBuffer() {
		return buffer;
	}

	/**
	 * Clears underlying buffer.
	 *
	 * @see ByteBuffer#clear()
	 */
	public void clear() {
		buffer.clear();
	}

	/**
	 * Consumes the buffer, sets its position to {@literal 0},
	 * and limits it to the amount of bytes written.
	 *
	 * @return Resulting buffer.
	 */
	public ByteBuffer consume() {
		ByteBuffer buffer = this.buffer;
		int pos = buffer.position();
		buffer.position(0);
		buffer.limit(pos);
		return buffer;
	}

	/**
	 * Ensures that {@literal size} amount
	 * of bytes can be written to the underlying buffer.
	 *
	 * @param size
	 * 		The amount of bytes needed to be written.
	 *
	 * @return Underlying buffer, with enough room to fit the data.
	 */
	public ByteBuffer ensureWriteable(int size) {
		ByteBuffer buffer = this.buffer;
		if (buffer.remaining() < size) {
			// A bit more room
			int pos = buffer.position();
			size = Integer.highestOneBit(pos + size - 1) << 1;
			ByteBuffer newBuffer = allocator.allocate(size);
			buffer.position(0).limit(pos);
			newBuffer.put(buffer);
			buffer = newBuffer;
			this.buffer = buffer;
		}
		return buffer;
	}
}
