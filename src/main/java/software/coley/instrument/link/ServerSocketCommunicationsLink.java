package software.coley.instrument.link;

import software.coley.instrument.Server;
import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.command.CommandConstants;
import software.coley.instrument.command.CommandFactory;

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
		clientSocket = serverSocket.accept();
	}

	@Override
	public void close() throws IOException {
		closeGracefully(clientSocket);
		clientSocket = null;
		if (!serverSocket.isClosed()) {
			serverSocket.close();
		}
	}

	@Override
	public void inputLoop(Server server) throws IOException {
		int key;
		DataInputStream is = new DataInputStream(clientSocket.getInputStream());
		while (!clientSocket.isClosed()) {
			// Read/handle commands
			key = is.read();
			AbstractCommand command = CommandFactory.create(key);
			if (command == null)
				throw new IllegalStateException("Failed to create command with type: " + key);
			command.read(is);
			command.handleServer(server);
		}
	}

	@Override
	public void send(byte[] message) throws IOException {
		if (clientSocket.isConnected())
			clientSocket.getOutputStream().write(message);
	}

	private void closeGracefully(Socket socket) throws IOException {
		if (socket != null && !socket.isClosed()) {
			OutputStream out = socket.getOutputStream();
			out.write(ID_COMMON_SHUTDOWN);
			out.flush();
			socket.close();
		}
	}
}
