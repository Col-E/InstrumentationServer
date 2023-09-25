package software.coley.instrument.util;

import java.util.concurrent.ThreadFactory;

/**
 * Basic thread factory which sets thread names.
 *
 * @author Matt Coley
 */
public final class NamedThreadFactory implements ThreadFactory {
	private static int counter = 0;
	private final String name;

	/**
	 * @param name
	 * 		Name to assign to thread.
	 */
	public NamedThreadFactory(String name) {
		this.name = name;
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread thread = new Thread(r);
		if (name != null)
			thread.setName(name + " " + counter++);
		return thread;
	}
}
