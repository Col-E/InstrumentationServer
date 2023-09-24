package software.coley.instrument.io;

import java.io.DataOutput;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

/**
 * {@link DataOutput} implementation backed by {@link ByteBufferSanitizer ByteBuffer}.
 *
 * @author xxDark
 */
public final class ByteBufferDataOutput implements DataOutput {
	private final ByteBufferSanitizer sanitizer;

	/**
	 * @param sanitizer
	 * 		Output buffer.
	 */
	public ByteBufferDataOutput(ByteBufferSanitizer sanitizer) {
		this.sanitizer = sanitizer;
	}

	/**
	 * @param alloc
	 * 		Buffer allocator.
	 */
	public ByteBufferDataOutput(ByteBufferAllocator alloc) {
		this(new ByteBufferSanitizer(alloc));
	}

	/**
	 * @return Underlying buffer.
	 */
	public ByteBuffer getBuffer() {
		return sanitizer.getBuffer();
	}

	/**
	 * Clears position, mark and limit of the underlying buffer.
	 *
	 * @see ByteBuffer#clear()
	 */
	public void reset() {
		sanitizer.clear();
	}

	/**
	 * @return Consumed buffer.
	 */
	public ByteBuffer consume() {
		return sanitizer.consume();
	}

	@Override
	public void write(int b) {
		buffer(1).put((byte) b);
	}

	@Override
	public void write(byte[] b) {
		buffer(b.length).put(b);
	}

	@Override
	public void write(byte[] b, int off, int len) {
		if ((off | len | (off + len) | (b.length - (off + len))) < 0) throw new IndexOutOfBoundsException();
		buffer(len).put(b, off, len);
	}

	@Override
	public void writeBoolean(boolean v) {
		buffer(1).put((byte) (v ? 1 : 0));
	}

	@Override
	public void writeByte(int v) {
		buffer(1).put((byte) v);
	}

	@Override
	public void writeShort(int v) {
		buffer(2).putShort((short) v);
	}

	@Override
	public void writeChar(int v) {
		buffer(2).putChar((char) v);
	}

	@Override
	public void writeInt(int v) {
		buffer(4).putInt(v);
	}

	@Override
	public void writeLong(long v) {
		buffer(8).putLong(v);
	}

	@Override
	public void writeFloat(float v) {
		buffer(4).putFloat(v);
	}

	@Override
	public void writeDouble(double v) {
		buffer(8).putDouble(v);
	}

	@Override
	public void writeBytes(String s) {
		int j = s.length();
		ByteBuffer buf = buffer(j);
		for (int i = 0; i < j; i++) {
			buf.put((byte) s.charAt(i));
		}
	}

	@Override
	public void writeChars(String s) {
		int j = s.length();
		ByteBuffer buf = buffer(j * 2);
		for (int i = 0; i < j; i++) {
			int v = s.charAt(i);
			buf.put((byte) (v >>> 8));
			buf.put((byte) (v & 0xFF));
		}
	}

	@Override
	public void writeUTF(String s) {
		CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
		CharBuffer cb = CharBuffer.wrap(s);

		// Each character will take up
		// at least 1 byte if all string is ASCII encoded,
		// so preallocate larger buffer.
		ByteBuffer buffer = buffer(4 + s.length());
		int position = buffer.position();

		// Add dummy length, it will be replaced
		// later after whole string is encoded.
		buffer.putInt(-1);
		while (true) {
			CoderResult result = encoder.encode(cb, buffer, true);
			if (result.isUnderflow()) {
				if (cb.hasRemaining()) {
					throw new IllegalStateException("Buffer must have no data left");
				}
				int newPosition = buffer.position();
				buffer.putInt(position, newPosition - position - 4);
				break;
			} else if (result.isOverflow()) {
				// Encoder might overflow if there are non-ASCII characters
				// in the string, take a guess of how much more data we have to encode.
				buffer = buffer(Math.max((int) encoder.averageBytesPerChar() * cb.remaining(), 64));
				continue;
			}
			throw new IllegalStateException("Unexpected coder result: " + result);
		}
	}

	private ByteBuffer buffer(int size) {
		return sanitizer.ensureWriteable(size);
	}
}
