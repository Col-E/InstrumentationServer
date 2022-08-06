package software.coley.instrument;

import software.coley.instrument.link.CommunicationsLink;
import software.coley.instrument.util.Logger;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Common logic between client/server.
 *
 * @param <Link>
 * 		Comm link impl type.
 *
 * @author Matt Coley
 */
@SuppressWarnings("rawtypes") // <?> causes problems on <Link ...>
public abstract class Entity<Link extends CommunicationsLink> {
	/**
	 * @return Backing communication link between client/server.
	 */
	public abstract Link getLink();

	/**
	 * Starts the input loop of the {@link #getLink() communication link}.
	 */
	@SuppressWarnings("unchecked")
	public void startInputLoop() {
		// TODO: If called multiple times (shouldn't happen)
		//  - cancel prior future (which should be handled semi-gracefully)
		//  - restart new one
		CompletableFuture.runAsync(() -> {
			try {
				Logger.debug("Begin input loop");
				getLink().inputLoop(this);
			} catch (IOException ex) {
				Logger.error("Error on input loop: " + ex);
				throw new RuntimeException(ex);
			}
		}, Executors.newSingleThreadExecutor());
	}

	/**
	 * Closes the client/server and shuts down the input loop of the {@link #getLink() communication link}.
	 */
	public void stopInputLoop() {
		try {
			Logger.debug("Stopping input loop");
			getLink().close();
		} catch (Exception ex) {
			// We're shutting down anyways so no big deal.
			Logger.warn("Error on stopping input loop" + ex);
		}
	}
}
