package software.coley.instrument.io;

import java.nio.ByteBuffer;

/**
 * Util for creating byte buffers.
 *
 * @author xxDark
 */
public interface ByteBufferAllocator {
	ByteBufferAllocator HEAP = ByteBuffer::allocate;
	ByteBufferAllocator DIRECT = ByteBuffer::allocateDirect;

	/**
	 * @param size
	 * 		Buffer size.
	 *
	 * @return New buffer.
	 */
	ByteBuffer allocate(int size);
}
