package software.coley.instrument.io;

import java.nio.Buffer;

/**
 * Compatibility for buffer operations changed across Java versions.
 * <hr>
 * Java 8
 * <pre>
 * {@code
 * public abstract class Buffer
 *     public final Buffer position(int newPosition)
 *
 * public abstract class ByteBuffer extends Buffer
 *     // nothing
 * }</pre>
 * Java 9+
 * <pre>
 * {@code
 * public abstract class Buffer
 *     public Buffer position(int newPosition);

 * public abstract class ByteBuffer extends Buffer
 *     	&#64;Override
 *     public Buffer position(int newPosition);
 * }</pre>
 */
public class ByteBufferCompat {
	/**
	 * @param buffer
	 * 		Buffer to set position of.
	 * @param position
	 * 		New position.
	 */
	public static void compatPosition(Buffer buffer, int position) {
		buffer.position(position);
	}
}
