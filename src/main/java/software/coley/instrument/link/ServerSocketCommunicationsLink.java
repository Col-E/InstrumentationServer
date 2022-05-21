package software.coley.instrument.link;

import software.coley.instrument.CommandConstants;
import software.coley.instrument.Server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * {@link ServerCommunicationsLink} implemented over {@link ServerSocket}/{@link Socket}.
 *
 * @author Matt Coley
 */
public class ServerSocketCommunicationsLink implements CommunicationsLink, ServerCommunicationsLink, CommandConstants {
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
		InputStream is = clientSocket.getInputStream();
		while (true) {
			key = is.read();
			if (key == ID_COMMON_STOP) {
				// Client is disconnecting, we can end the input loop.
				closeGracefully(clientSocket);
				return;
			} else {
				// TODO: Length should be more than a single byte.
				//  - What size makes sense?
				//    - Consider we want to eventually transfer 'class bytecode' which can be megabytes in extreme cases.
				//    - Want to keep the code clean
				int length = is.read();
				switch (key) {
					// TODO: What commands should we offer?
				}
			}
		}
	}

	private void closeGracefully(Socket socket) throws IOException {
		if (socket != null && !socket.isClosed()) {
			OutputStream out = socket.getOutputStream();
			out.write(ID_COMMON_STOP);
			out.flush();
			socket.close();
		}
	}
}
