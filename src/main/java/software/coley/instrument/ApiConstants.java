package software.coley.instrument;

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
}
