package software.coley.instrument.io;

import java.io.DataInput;
import java.io.IOException;
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
		buffer.position(skipped);
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

	@Override
	public String readUTF() throws IOException {
		ByteBuffer buffer = this.buffer;
		int len = buffer.getInt();
		ByteBuffer slice = buffer.slice().order(buffer.order());
		slice.limit(len);
		CharBuffer cb;
		try {
			cb = StandardCharsets.UTF_8.newDecoder().decode(slice);
		} catch (CharacterCodingException ex) {
			throw new IOException(ex);
		}
		return cb.toString();
	}
}
