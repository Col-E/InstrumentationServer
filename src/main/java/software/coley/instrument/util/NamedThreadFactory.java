package software.coley.instrument.util;

import java.util.concurrent.ThreadFactory;

public final class NamedThreadFactory implements ThreadFactory {
	private final String name;

	public NamedThreadFactory(String name) {
		this.name = name;
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread thread = new Thread(r);
		if (name != null)
			thread.setName(name);
		return thread;
	}
}
