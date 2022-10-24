package software.coley.instrument;

import software.coley.instrument.message.broadcast.AbstractBroadcastMessage;

/**
 * Various constants for easy access.
 */
public class ApiConstants {
	/**
	 * ID for the bootstrap classloader.
	 * <br>
	 * Should contain classes from {@code rt.jar} or the core modules depending on the target Java version.
	 */
	public static final int BOOTSTRAP_CLASSLOADER_ID = 0;
	/**
	 * ID for the system classloader
	 * <br>
	 * Should contain application classes,
	 */
	public static final int SYSTEM_CLASSLOADER_ID = 1;
	/**
	 * Key message ID to indicate the received message is a
	 * {@link AbstractBroadcastMessage}.
	 */
	public static final int BROADCAST_MESSAGE_ID = -1;
}
