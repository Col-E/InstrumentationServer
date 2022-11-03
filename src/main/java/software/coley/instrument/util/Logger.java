package software.coley.instrument.util;

/**
 * Debugging utility without requiring dependencies.
 *
 * @author Matt Coley
 */
public class Logger {
	public static final int NONE = -1;
	public static final int ERROR = 0;
	public static final int WARN = 1;
	public static final int INFO = 2;
	public static final int DEBUG = 3;
	public static int level = NONE;
	public static String prefix = "[Client]";

	/**
	 * @param message
	 * 		Debug message.
	 */
	public static void debug(String message) {
		if (level >= DEBUG)
			System.out.println(fmt(message));
	}

	/**
	 * @param message
	 * 		Info message.
	 */
	public static void info(String message) {
		if (level >= INFO)
			System.out.println(fmt(message));
	}

	/**
	 * @param message
	 * 		Warning message.
	 */
	public static void warn(String message) {
		if (level >= WARN)
			System.err.println(fmt(message));
	}

	/**
	 * @param message
	 * 		Error message.
	 */
	public static void error(String message) {
		if (level >= ERROR)
			System.err.println(fmt(message));
	}

	private static String fmt(String message) {
		return prefix +String.format("%-12s " + message, "[" + Thread.currentThread().getName() + "]");
	}
}
