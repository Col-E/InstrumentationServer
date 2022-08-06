package software.coley.instrument.command;

import software.coley.instrument.Client;
import software.coley.instrument.Server;

import java.io.DataInputStream;
import java.io.IOException;

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
	 * Called after {@link #read(DataInputStream)}.
	 * Used on the client side to handle this packet when it is received from a server.
	 *
	 * @param client
	 * 		The client instance.
	 */
	public abstract void handleClient(Client client);

	/**
	 * Called after {@link #read(DataInputStream)}.
	 * Used on the server side to handle this packet when it is received from a client.
	 *
	 * @param server
	 * 		The server instance.
	 *
	 * @throws IOException
	 * 		Most commands attempt to send automatic replies to clients.
	 * 		Thrown when that operation fails.
	 */
	public abstract void handleServer(Server server) throws IOException;

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
	 * @param in
	 * 		Input stream to read from.
	 *
	 * @throws IOException
	 * 		When the stream cannot be read from.
	 */
	public void read(DataInputStream in) throws IOException {
		// no-op by default
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
