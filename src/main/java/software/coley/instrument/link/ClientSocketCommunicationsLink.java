package software.coley.instrument.link;

import software.coley.instrument.Client;
import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.command.CommandConstants;
import software.coley.instrument.command.CommandFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * {@link CommunicationsLink} implemented over {@link ServerSocket}/{@link Socket} for {@link Client}s.
 *
 * @author Matt Coley
 */
public class ClientSocketCommunicationsLink implements CommunicationsLink<Client>, CommandConstants {
	private final Socket socket;

	/**
	 * @param ip
	 * 		Server ip to connect to.
	 * @param port
	 * 		Port to operate on.
	 *
	 * @throws IOException
	 * 		When a no connection to the ip/port could be made.
	 */
	public ClientSocketCommunicationsLink(String ip, int port) throws IOException {
		this.socket = new Socket(ip, port);
	}

	@Override
	public void inputLoop(Client client) throws IOException {
		int key;
		DataInputStream is = new DataInputStream(socket.getInputStream());
		while (!socket.isClosed()) {
			// Read/handle commands
			key = is.read();
			AbstractCommand command = CommandFactory.create(key);
			if (command == null)
				throw new IllegalStateException("Failed to create command with type: " + key);
			command.read(is);
			command.handleClient(client);
		}
	}

	@Override
	public void send(byte[] message) throws IOException {
		if (socket.isConnected())
			socket.getOutputStream().write(message);
	}

	@Override
	public void open() {
		// no-op, done in constructor
	}

	@Override
	public void close() throws IOException {
		socket.getOutputStream().write(ID_COMMON_DISCONNECT);
		socket.close();
	}
}
