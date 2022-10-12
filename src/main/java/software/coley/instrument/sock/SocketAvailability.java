package software.coley.instrument.sock;

import java.net.ServerSocket;
import java.util.Random;

/**
 * Small util for determining port availability.
 *
 * @author Matt Coley
 */
public final class SocketAvailability {
	private static final int PORT_RANGE_MIN = 16384; // 2^14 - By this point, port usage drops off significantly.
	private static final int PORT_RANGE_MAX = 65535; // 2^16
	private static final int PORT_RANGE = PORT_RANGE_MAX - PORT_RANGE_MIN;
	private static final int MAX_ATTEMPTS = 1000;
	private static final Random random = new Random();

	private SocketAvailability() {
	}

	/**
	 * @return Random available port.
	 * {@code -1} when no available port is found.
	 */
	public static int findAvailable() {
		int attempts = 0;
		while (attempts < MAX_ATTEMPTS) {
			int port = PORT_RANGE_MIN + random.nextInt(PORT_RANGE + 1);
			if (isAvailable(port))
				return port;
			attempts++;
		}
		return -1;
	}

	/**
	 * @param port
	 * 		Port to check.
	 *
	 * @return {@code true} when can be bound to.
	 */
	@SuppressWarnings("unused")
	private static boolean isAvailable(int port) {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
}