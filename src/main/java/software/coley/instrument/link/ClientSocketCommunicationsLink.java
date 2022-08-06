package software.coley.instrument.link;

import software.coley.instrument.Client;
import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.command.CommandConstants;
import software.coley.instrument.command.CommandFactory;
import software.coley.instrument.util.Logger;

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
			try {
				// Read/handle commands
				Logger.debug("Waiting for next command");
				key = is.read();
				AbstractCommand command = CommandFactory.create(key);
				if (command == null) {
					Logger.debug("Received unknown command key: " + key);
					throw new IllegalStateException("Failed to create command with type: " + key);
				}
				command.read(is);
				Logger.debug("Received command: " + command);
				command.handleClient(client);
			} catch (IOException ex) {
				Logger.error("Input loop encountered error: " + ex);
				throw ex;
			}
		}
	}

	@Override
	public void send(AbstractCommand command) throws IOException {
		if (socket.isConnected()) {
			Logger.debug("Sending command: " + command);
			try {
				socket.getOutputStream().write(command.generate());
			} catch (IOException ex) {
				Logger.error("Failed to send command: " + ex);
				throw ex;
			}
		} else {
			Logger.warn("Cannot send command, socket is closed");
		}
	}

	@Override
	public void open() {
		// no-op, done in constructor
	}

	@Override
	public void close() throws IOException {
		Logger.debug("Closing server connection...");
		socket.getOutputStream().write(ID_COMMON_DISCONNECT);
		socket.close();
		Logger.debug("Client closed");
	}
}
