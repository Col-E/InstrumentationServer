package software.coley.instrument.command;

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
	 */
	public void read(ByteBuffer buffer) {
		// no-op by default
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
