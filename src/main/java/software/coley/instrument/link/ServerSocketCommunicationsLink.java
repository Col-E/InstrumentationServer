package software.coley.instrument.link;

import software.coley.instrument.Server;
import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.command.CommandConstants;
import software.coley.instrument.command.CommandFactory;
import software.coley.instrument.util.Logger;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * {@link CommunicationsLink} implemented over {@link ServerSocket}/{@link Socket} for {@link Server}s.
 *
 * @author Matt Coley
 */
public class ServerSocketCommunicationsLink implements CommunicationsLink<Server>, CommandConstants {
	private final ServerSocket serverSocket;
	private Socket clientSocket;

	/**
	 * @param port
	 * 		Port to operate on.
	 *
	 * @throws IOException
	 * 		When a {@link ServerSocket} on the given port could not be created.
	 */
	public ServerSocketCommunicationsLink(int port) throws IOException {
		this.serverSocket = new ServerSocket(port);
	}

	@Override
	public void open() throws IOException {
		closeGracefully(clientSocket);
		Logger.debug("Awaiting client connection...");
		clientSocket = serverSocket.accept();
		Logger.debug("Client connected: " + clientSocket.getInetAddress());
	}

	@Override
	public void close() throws IOException {
		closeGracefully(clientSocket);
		clientSocket = null;
		if (!serverSocket.isClosed())
			serverSocket.close();
		Logger.debug("Server closed");
	}

	@Override
	public void inputLoop(Server server) throws IOException {
		int key;
		DataInputStream is = new DataInputStream(clientSocket.getInputStream());
		while (!clientSocket.isClosed()) {
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
				command.handleServer(server);
			} catch (IOException ex) {
				Logger.error("Input loop encountered error: " + ex);
				throw ex;
			}
		}
	}

	@Override
	public void send(AbstractCommand command) throws IOException {
		if (clientSocket.isConnected()) {
			Logger.debug("Sending command: " + command);
			try {
				clientSocket.getOutputStream().write(command.generate());
			} catch (IOException ex) {
				Logger.error("Failed to send command: " + ex);
				throw ex;
			}
		} else {
			Logger.warn("Cannot send command, socket is closed");
		}
	}

	private void closeGracefully(Socket socket) throws IOException {
		if (socket != null && !socket.isClosed()) {
			Logger.debug("Closing client socket...");
			OutputStream out = socket.getOutputStream();
			out.write(ID_COMMON_SHUTDOWN);
			out.flush();
			socket.close();
		}
	}
}
