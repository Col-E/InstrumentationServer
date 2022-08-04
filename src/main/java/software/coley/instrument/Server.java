package software.coley.instrument;

import software.coley.instrument.link.CommunicationsLink;

import java.lang.instrument.Instrumentation;

/**
 * Server which exposes capabilities of {@link Instrumentation} to a client.
 *
 * @author Matt Coley
 */
public class Server extends Entity<CommunicationsLink<Server>> {
	public static final int DEFAULT_PORT = 25252;
	private final Instrumentation instrumentation;
	private final CommunicationsLink<Server> link;

	/**
	 * @param instrumentation
	 * 		Instrumentation instance.
	 * @param link
	 * 		Communications link.
	 */
	public Server(Instrumentation instrumentation, CommunicationsLink<Server> link) {
		this.instrumentation = instrumentation;
		this.link = link;
	}

	/**
	 * @return The magic thingy.
	 */
	public Instrumentation getInstrumentation() {
		return instrumentation;
	}

	@Override
	public CommunicationsLink<Server> getLink() {
		return link;
	}
}
