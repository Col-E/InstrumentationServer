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

	public ByteBufferSanitizer(ByteBufferAllocator allocator) {
		this.allocator = allocator;
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}

	public void clear() {
		buffer.clear();
	}

	public ByteBuffer consume() {
		ByteBuffer buffer = this.buffer;
		int pos = buffer.position();
		buffer.position(0);
		buffer.limit(pos);
		return buffer;
	}

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
