package software.coley.instrument.io;

import java.io.DataInput;
import java.io.UncheckedIOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

/**
 * {@link DataInput} implementation backed by {@link ByteBuffer}.
 *
 * @author xxDark
 */
public final class ByteBufferDataInput implements DataInput {
	private final ByteBuffer buffer;

	public ByteBufferDataInput(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public void readFully(byte[] b) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void readFully(byte[] b, int off, int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int skipBytes(int n) {
		ByteBuffer buffer = this.buffer;
		int position = buffer.position();
		int skipped = Math.min(buffer.limit(), position + n);
		ByteBufferCompat.compatPosition(buffer, skipped);
		return buffer.position() - position;
	}

	@Override
	public boolean readBoolean() {
		return buffer.get() != 0;
	}

	@Override
	public byte readByte() {
		return buffer.get();
	}

	@Override
	public int readUnsignedByte() {
		return buffer.get() & 0xFF;
	}

	@Override
	public short readShort() {
		return buffer.getShort();
	}

	@Override
	public int readUnsignedShort() {
		return buffer.getShort() & 0xFF;
	}

	@Override
	public char readChar() {
		return buffer.getChar();
	}

	@Override
	public int readInt() {
		return buffer.getInt();
	}

	@Override
	public long readLong() {
		return buffer.getLong();
	}

	@Override
	public float readFloat() {
		return buffer.getFloat();
	}

	@Override
	public double readDouble() {
		return buffer.getDouble();
	}

	@Override
	public String readLine() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws UncheckedIOException
	 * 		If any I/O error is thrown. See comment in catch block.
	 * @see UncheckedIOException#getCause()
	 */
	@Override
	@SuppressWarnings("RedundantCast")
	public String readUTF() {
		ByteBuffer buffer = this.buffer;
		int len = buffer.getInt();
		// Calling slice allows us to limit and set position to 0,
		// so we can update the actual buffer after decoding is done.
		ByteBuffer slice = buffer.slice().order(buffer.order());
		((Buffer) slice).limit(len);
		CharBuffer cb;
		try {
			cb = StandardCharsets.UTF_8.newDecoder().decode(slice);
		} catch (CharacterCodingException ex) {
			// This should not occur since default decoder
			// replaces invalid characters, but we will throw
			// unchecked I/O exception just in case.
			throw new UncheckedIOException(ex);
		}
		// We need to move position of the original buffer accordingly,
		// otherwise it will cause later invocations to error.
		ByteBufferCompat.compatPosition(buffer, buffer.position() + slice.position());
		return cb.toString();
	}
}
