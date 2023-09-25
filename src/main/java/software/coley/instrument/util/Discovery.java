package software.coley.instrument.util;

import java.util.Properties;

/**
 * Places the port of the active {@link software.coley.instrument.Server} in a discoverable place
 * by users with the attachment API. This allows us to check if a remote VM already has the server running and
 * what port it is on.
 *
 * @author Matt Coley
 */
public class Discovery {
	private static final String PREFIX = "commons-discovery-";
	private static final String SUFFIX = ".jar";

	/**
	 * @param properties
	 * 		Some properties.
	 *
	 * @return Port stored in properties. {@code -1} if no port was stored.
	 */
	public static int extractPort(Properties properties) {
		String path = properties.getProperty("java.class.path", null);
		if (path == null || path.isEmpty()) {
			return -1;
		} else {
			int last = path.lastIndexOf(PREFIX);
			if (last > 0 && path.lastIndexOf(SUFFIX) > last) {
				path = path.substring(last + PREFIX.length(), path.length() - SUFFIX.length());
				try {
					return Integer.parseInt(path);
				} catch (NumberFormatException ignored) {
					// False positive
				}
			}
		}
		return -1;
	}

	/**
	 * @param port
	 * 		Port to broadcast on.
	 *
	 * @return Name to use for discovery file.
	 */
	public static String createDiscoveryName(int port) {
		return PREFIX + port + SUFFIX;
	}

	/**
	 * Update the system properties to add a hint of what port we're running on.
	 *
	 * @param port
	 * 		Port to broadcast on.
	 */
	public static void setupDiscovery(int port) {
		String path = System.getProperty("java.class.path", null);
		String discoveryName = createDiscoveryName(port);
		if (path == null || path.isEmpty()) {
			path = discoveryName;
		} else if (!path.contains(discoveryName)) {
			path += ";" + discoveryName;
		}
		System.setProperty("java.class.path", path);
	}

	/**
	 * Update the system properties to remove the port hint.
	 *
	 * @param port
	 * 		Port to broadcast on.
	 */
	public static void removeDiscovery(int port) {
		String path = System.getProperty("java.class.path", null);
		String discoveryName = createDiscoveryName(port);
		if (path != null && path.contains(discoveryName)) {
			int newLength = path.length() - discoveryName.length();
			if (newLength > 0 && path.charAt(newLength - 1) == ';')
				newLength--;
			path = path.substring(0, newLength);
			System.setProperty("java.class.path", path);
		}
	}
}
