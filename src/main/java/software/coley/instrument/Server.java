package software.coley.instrument;

import software.coley.instrument.link.ServerCommunicationsLink;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Server which exposes capabilities of {@link Instrumentation} to a client.
 * Communication with a client is abstracted over {@link ServerCommunicationsLink}.
 *
 * @author Matt Coley
 */
public class Server {
	public static final int DEFAULT_PORT = 25252;
	private final Instrumentation instrumentation;
	private final ServerCommunicationsLink link;

	/**
	 * @param instrumentation
	 * 		Instrumentation instance.
	 * @param link
	 * 		Communications link.
	 */
	public Server(Instrumentation instrumentation, ServerCommunicationsLink link) {
		this.instrumentation = instrumentation;
		this.link = link;
	}

	/**
	 * @return Backing communications link.
	 */
	public ServerCommunicationsLink getLink() {
		return link;
	}

	/**
	 * Starts the input loop of the {@link #getLink() communication link}.
	 */
	public void startInputLoop() {
		// TODO: If called multiple times (shouldn't happen)
		//  - cancel prior future (which should be handled semi-gracefully)
		//  - restart new one
		CompletableFuture.runAsync(() -> {
			try {
				link.inputLoop(this);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}, Executors.newSingleThreadExecutor());
	}

	/**
	 * Closes the server and shuts down the input loop of the {@link #getLink() communication link}.
	 */
	public void stopInputLoop() {
		try {
			link.close();
		} catch (Exception ignored) {
			// We're shutting down anyways so no big deal.
		}
	}
}
