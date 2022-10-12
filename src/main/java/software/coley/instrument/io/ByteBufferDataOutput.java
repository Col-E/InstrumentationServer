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

	public ByteBufferDataOutput(ByteBufferSanitizer sanitizer) {
		this.sanitizer = sanitizer;
	}

	public ByteBufferDataOutput(ByteBufferAllocator alloc) {
		this(new ByteBufferSanitizer(alloc));
	}

	public ByteBuffer getBuffer() {
		return sanitizer.getBuffer();
	}

	public void reset() {
		sanitizer.clear();
	}

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
		if ((off | len | (off + len) | (b.length - (off + len))) < 0)
			throw new IndexOutOfBoundsException();
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
		ByteBuffer buffer = buffer(4);
		int position = buffer.position();
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
				buffer = buffer(256);
				continue;
			}
			throw new IllegalStateException("Unexpected coder result: " + result);
		}
	}

	private ByteBuffer buffer(int size) {
		return sanitizer.ensureWriteable(size);
	}
}