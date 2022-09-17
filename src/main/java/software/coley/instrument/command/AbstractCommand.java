package software.coley.instrument.command;

import software.coley.instrument.Client;
import software.coley.instrument.Server;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Command outline.
 */
public abstract class AbstractCommand implements CommandConstants {
	private final int key;

	/**
	 * @param key
	 * 		Command identifier.
	 */
	protected AbstractCommand(int key) {
		this.key = key;
	}

	/**
	 * @return Command identifier.
	 */
	public int key() {
		return key;
	}

	/**
	 * @return Byte array representation of command.
	 */
	public abstract byte[] generate();

	/**
	 * @param buffer
	 * 		Buffer to read from.
	 *
	 * @throws IOException
	 * 		When the buffer cannot be read from.
	 */
	public void read(ByteBuffer buffer) throws IOException {
		// no-op by default
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
